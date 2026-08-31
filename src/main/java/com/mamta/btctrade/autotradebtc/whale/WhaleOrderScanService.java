package com.mamta.btctrade.autotradebtc.whale;

import com.mamta.btctrade.autotradebtc.exchange.ExchangeName;
import com.mamta.btctrade.autotradebtc.exchange.ExchangeOrderBookClient;
import com.mamta.btctrade.autotradebtc.exchange.ExchangeOrderBookLevel;
import com.mamta.btctrade.autotradebtc.exchange.ExchangeOrderBookSnapshot;
import com.mamta.btctrade.autotradebtc.whale.dto.WhaleLimitOrderResponse;
import com.mamta.btctrade.autotradebtc.whale.dto.WhaleOrderScanStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Periodically polls every configured exchange's BTC order book for resting price levels whose
 * notional value crosses {@code app.whale.order.min-notional-usd}, and tracks their lifecycle
 * (ACTIVE while still resting, REMOVED once they drop back out) - the same "large orderbook
 * statistics" idea as Coinglass's whale-order tracker
 * (https://www.coinglass.com/large-orderbook-statistics), self-hosted across the exchanges wired
 * up in the {@code exchange} package.
 *
 * A level only starts being tracked once it reaches the full threshold, but isn't dropped again
 * until it falls under {@link #REMOVE_HYSTERESIS_RATIO} of it - without this, a level sitting
 * right at the threshold would flap ACTIVE/REMOVED on every scan.
 */
@Service
public class WhaleOrderScanService {

    private static final Logger log = LoggerFactory.getLogger(WhaleOrderScanService.class);
    private static final BigDecimal REMOVE_HYSTERESIS_RATIO = new BigDecimal("0.5");
    private static final String SYMBOL = "BTC";

    private final List<ExchangeOrderBookClient> exchangeClients;
    private final WhaleLimitOrderRepository repository;
    private final BigDecimal minNotionalUsd;
    private final int retentionDays;

    private volatile Instant lastScanAt;
    private final Map<String, String> lastScanErrors = new ConcurrentHashMap<>();

    public WhaleOrderScanService(
            List<ExchangeOrderBookClient> exchangeClients,
            WhaleLimitOrderRepository repository,
            @Value("${app.whale.order.min-notional-usd:1000000}") BigDecimal minNotionalUsd,
            @Value("${app.whale.order.retention-days:30}") int retentionDays) {
        this.exchangeClients = exchangeClients;
        this.repository = repository;
        this.minNotionalUsd = minNotionalUsd;
        this.retentionDays = retentionDays;
    }

    @Scheduled(fixedRateString = "${app.whale.order.scan-interval-ms:15000}")
    public void scheduledScan() {
        scanNow();
    }

    public void scanNow() {
        for (ExchangeOrderBookClient client : exchangeClients) {
            String exchangeKey = client.exchange().name();
            try {
                scanExchange(client);
                lastScanErrors.remove(exchangeKey);
            } catch (Exception e) {
                lastScanErrors.put(exchangeKey, e.getMessage());
                log.warn("Whale order scan failed for {}: {}", exchangeKey, e.getMessage());
            }
        }
        lastScanAt = Instant.now();
    }

    private void scanExchange(ExchangeOrderBookClient client) {
        ExchangeOrderBookSnapshot snapshot = client.fetchBtcOrderBook();
        if (snapshot == null) {
            return;
        }
        Instant now = Instant.now();
        String exchange = client.exchange().name();
        reconcileSide(exchange, "BUY", snapshot.bids(), now);
        reconcileSide(exchange, "SELL", snapshot.asks(), now);
    }

    private void reconcileSide(String exchange, String side, List<ExchangeOrderBookLevel> levels, Instant now) {
        BigDecimal removeThreshold = minNotionalUsd.multiply(REMOVE_HYSTERESIS_RATIO);
        List<WhaleLimitOrder> tracked = repository.findByExchangeAndSideAndStatus(exchange, side, WhaleOrderStatus.ACTIVE);
        List<WhaleLimitOrder> stillResting = new ArrayList<>();

        for (ExchangeOrderBookLevel level : levels) {
            BigDecimal notional = level.price().multiply(level.size());
            if (notional.compareTo(minNotionalUsd) < 0) {
                continue;
            }
            WhaleLimitOrder existing = findByPrice(tracked, level.price());
            if (existing != null) {
                existing.setSize(level.size());
                existing.setNotionalUsd(notional);
                existing.setLastSeenAt(now);
                repository.save(existing);
                stillResting.add(existing);
            } else {
                WhaleLimitOrder created = new WhaleLimitOrder(exchange, SYMBOL, side, level.price(), level.size(), notional, now);
                repository.save(created);
                log.info("New whale limit order: {} {} {} BTC {} @ {} (${})",
                        exchange, side, level.size(), SYMBOL, level.price(), notional);
            }
        }

        for (WhaleLimitOrder order : tracked) {
            if (stillResting.contains(order)) {
                continue;
            }
            ExchangeOrderBookLevel current = findLevel(levels, order.getPrice());
            BigDecimal currentNotional = current == null ? BigDecimal.ZERO : current.price().multiply(current.size());
            if (currentNotional.compareTo(removeThreshold) < 0) {
                order.setStatus(WhaleOrderStatus.REMOVED);
                order.setRemovedAt(now);
            } else {
                // Below the full threshold but still within the hysteresis band - stays ACTIVE.
                order.setSize(current.size());
                order.setNotionalUsd(currentNotional);
                order.setLastSeenAt(now);
            }
            repository.save(order);
        }
    }

    private static WhaleLimitOrder findByPrice(List<WhaleLimitOrder> orders, BigDecimal price) {
        for (WhaleLimitOrder order : orders) {
            if (order.getPrice().compareTo(price) == 0) {
                return order;
            }
        }
        return null;
    }

    private static ExchangeOrderBookLevel findLevel(List<ExchangeOrderBookLevel> levels, BigDecimal price) {
        for (ExchangeOrderBookLevel level : levels) {
            if (level.price().compareTo(price) == 0) {
                return level;
            }
        }
        return null;
    }

    /** Trims REMOVED orders older than the retention window so the table doesn't grow unbounded. */
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupOldRemovedOrders() {
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        long deleted = repository.deleteByStatusAndRemovedAtBefore(WhaleOrderStatus.REMOVED, cutoff);
        if (deleted > 0) {
            log.info("Cleaned up {} whale limit orders removed more than {} days ago", deleted, retentionDays);
        }
    }

    public List<WhaleLimitOrderResponse> listOrders(String exchangeFilter, String sideFilter, boolean includeRemoved) {
        return repository.findAllByOrderByNotionalUsdDesc().stream()
                .filter(o -> exchangeFilter == null || o.getExchange().equalsIgnoreCase(exchangeFilter))
                .filter(o -> sideFilter == null || o.getSide().equalsIgnoreCase(sideFilter))
                .filter(o -> includeRemoved || o.getStatus() == WhaleOrderStatus.ACTIVE)
                .map(WhaleOrderScanService::toResponse)
                .toList();
    }

    public WhaleOrderScanStatus status() {
        Map<String, Long> activeByExchange = repository.findByStatus(WhaleOrderStatus.ACTIVE).stream()
                .collect(Collectors.groupingBy(WhaleLimitOrder::getExchange, LinkedHashMap::new, Collectors.counting()));
        long activeCount = activeByExchange.values().stream().mapToLong(Long::longValue).sum();
        return new WhaleOrderScanStatus(lastScanAt, minNotionalUsd, activeCount, activeByExchange, Map.copyOf(lastScanErrors));
    }

    private static WhaleLimitOrderResponse toResponse(WhaleLimitOrder o) {
        return new WhaleLimitOrderResponse(
                o.getId(), o.getExchange(), o.getSymbol(), o.getSide(), o.getPrice(), o.getSize(),
                o.getNotionalUsd(), o.getStatus().name(), o.getFirstSeenAt(), o.getLastSeenAt(), o.getRemovedAt());
    }
}

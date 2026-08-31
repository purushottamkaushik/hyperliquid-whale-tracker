package com.mamta.btctrade.autotradebtc.wallet;

import com.mamta.btctrade.autotradebtc.hyperliquid.HyperliquidClient;
import com.mamta.btctrade.autotradebtc.hyperliquid.dto.HyperliquidFillDto;
import com.mamta.btctrade.autotradebtc.hyperliquid.dto.HyperliquidOpenOrderDto;
import com.mamta.btctrade.autotradebtc.hyperliquid.dto.HyperliquidPositionDto;
import com.mamta.btctrade.autotradebtc.wallet.dto.WalletOverviewResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Every {@code app.wallet.sync-interval-ms}, refreshes BTC-only trading data (open orders, open
 * position, realized/unrealized PnL) for the next {@code app.wallet.sync-batch-size} tracked
 * Hyperliquid wallets - oldest-attempted first, so every wallet gets a turn in round-robin order -
 * and persists the latest snapshot, so the wallets screen reads from the database instead of
 * calling Hyperliquid on every page load.
 *
 * <p>Each wallet costs 3 sequential Hyperliquid calls and {@link HyperliquidClient} throttles all
 * of them to roughly 1/sec to avoid its aggressive rate limiting (empirically, more than one
 * request per second starts returning 429s). Attempting every tracked wallet in a single run - as
 * this used to - meant hundreds of wallets each cycle, which took far longer than the interval
 * (causing overlapping/starved runs) and still drew 429s under that sustained load; batching keeps
 * each run's duration and request volume small and predictable regardless of how many wallets are
 * tracked in total.
 */
@Service
public class WalletSyncService {

    private static final String BTC = "BTC";
    private static final Logger log = LoggerFactory.getLogger(WalletSyncService.class);

    private final WalletRepository walletRepository;
    private final HyperliquidClient hyperliquidClient;
    private final WalletStatRepository walletStatRepository;
    private final WalletBtcPositionRepository positionRepository;
    private final WalletBtcOpenOrderRepository openOrderRepository;
    private final int batchSize;

    public WalletSyncService(
            WalletRepository walletRepository,
            HyperliquidClient hyperliquidClient,
            WalletStatRepository walletStatRepository,
            WalletBtcPositionRepository positionRepository,
            WalletBtcOpenOrderRepository openOrderRepository,
            @Value("${app.wallet.sync-batch-size:10}") int batchSize) {
        this.walletRepository = walletRepository;
        this.hyperliquidClient = hyperliquidClient;
        this.walletStatRepository = walletStatRepository;
        this.positionRepository = positionRepository;
        this.openOrderRepository = openOrderRepository;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedRateString = "${app.wallet.sync-interval-ms:120000}")
    public void syncNow() {
        List<Wallet> batch = walletRepository.findByChainAndDeactivatedAtIsNullAndDeletedAtIsNullOrderByLastSyncAttemptAtAsc(
                Chain.HYPERLIQUID, PageRequest.of(0, batchSize));
        BigDecimal btcPrice = hyperliquidClient.fetchBtcMidPrice();
        for (Wallet wallet : batch) {
            try {
                syncWallet(wallet, btcPrice);
            } catch (Exception e) {
                Throwable rootCause = e.getCause() != null ? e.getCause() : e;
                log.warn(
                        "Failed to sync BTC data for wallet {}: {} (root cause: {}: {})",
                        wallet.getAddress(), e.getMessage(),
                        rootCause.getClass().getSimpleName(), rootCause.getMessage());
            } finally {
                // Recorded regardless of outcome so a wallet that keeps failing rotates to the
                // back of the queue instead of being retried every cycle at everyone else's cost.
                wallet.setLastSyncAttemptAt(Instant.now());
                walletRepository.save(wallet);
            }
        }
    }

    private void syncWallet(Wallet wallet, BigDecimal btcPrice) {
        Long walletId = wallet.getId();
        String address = wallet.getAddress();

        List<HyperliquidPositionDto> btcPositions = hyperliquidClient.fetchPositions(address).stream()
                .filter(p -> BTC.equals(p.coin()))
                .toList();
        List<HyperliquidOpenOrderDto> btcOpenOrders = hyperliquidClient.fetchOpenOrders(address).stream()
                .filter(o -> BTC.equals(o.coin()))
                .toList();
        BigDecimal realizedPnl = hyperliquidClient.fetchFills(address).stream()
                .filter(f -> BTC.equals(f.coin()))
                .map(HyperliquidFillDto::closedPnl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal unrealizedPnl = btcPositions.stream()
                .map(HyperliquidPositionDto::unrealizedPnl)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        WalletStat stat = walletStatRepository.findByWalletId(walletId)
                .orElseGet(() -> new WalletStat(walletId, null, null));
        stat.setTotalPnl(realizedPnl);
        stat.setUnrealizedPnl(unrealizedPnl);
        stat.setTotalPnlBtc(toBtc(realizedPnl, btcPrice));
        stat.setUnrealizedPnlBtc(toBtc(unrealizedPnl, btcPrice));
        stat.setUpdatedAt(Instant.now());
        walletStatRepository.save(stat);

        positionRepository.deleteByWalletId(walletId);
        btcPositions.forEach(p -> positionRepository.save(new WalletBtcPosition(walletId, p)));

        openOrderRepository.deleteByWalletId(walletId);
        btcOpenOrders.forEach(o -> openOrderRepository.save(new WalletBtcOpenOrder(walletId, o)));
    }

    /** Converts a USD PnL figure to BTC at {@code btcPrice}; null if either input is unavailable. */
    private static BigDecimal toBtc(BigDecimal usdAmount, BigDecimal btcPrice) {
        if (usdAmount == null || btcPrice == null || btcPrice.signum() == 0) {
            return null;
        }
        return usdAmount.divide(btcPrice, 10, RoundingMode.HALF_UP);
    }

    /**
     * Combines every tracked wallet with its latest persisted BTC snapshot. A Hyperliquid wallet
     * not yet synced (or with no BTC activity) gets null PnL and empty lists; a BTC-chain wallet
     * always does, since positions/open orders/PnL are Hyperliquid-only concepts.
     */
    public List<WalletOverviewResponse> getOverview() {
        List<Wallet> wallets = walletRepository.findByDeletedAtIsNull();
        Map<Long, WalletStat> statsByWalletId = walletStatRepository.findAll().stream()
                .collect(Collectors.toMap(WalletStat::getWalletId, Function.identity()));
        Map<Long, List<HyperliquidPositionDto>> positionsByWalletId = positionRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        WalletBtcPosition::getWalletId,
                        Collectors.mapping(WalletSyncService::toDto, Collectors.toList())));
        Map<Long, List<HyperliquidOpenOrderDto>> openOrdersByWalletId = openOrderRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        WalletBtcOpenOrder::getWalletId,
                        Collectors.mapping(WalletSyncService::toDto, Collectors.toList())));

        return wallets.stream()
                .map(w -> {
                    WalletStat stat = statsByWalletId.get(w.getId());
                    return new WalletOverviewResponse(
                            w.getId(),
                            w.getAddress(),
                            w.getLabel(),
                            w.getChain(),
                            w.getSource(),
                            w.getCreatedAt(),
                            Boolean.TRUE.equals(w.getMarked()),
                            w.getDeactivatedAt() == null,
                            stat != null ? stat.getTotalPnl() : null,
                            stat != null ? stat.getUnrealizedPnl() : null,
                            stat != null ? stat.getTotalPnlBtc() : null,
                            stat != null ? stat.getUnrealizedPnlBtc() : null,
                            stat != null ? stat.getUpdatedAt() : null,
                            openOrdersByWalletId.getOrDefault(w.getId(), List.of()),
                            positionsByWalletId.getOrDefault(w.getId(), List.of()));
                })
                .toList();
    }

    private static HyperliquidPositionDto toDto(WalletBtcPosition p) {
        return new HyperliquidPositionDto(
                p.getCoin(), p.getSide(), p.getSize(), p.getEntryPrice(), p.getPositionValue(),
                p.getUnrealizedPnl(), p.getLeverage());
    }

    private static HyperliquidOpenOrderDto toDto(WalletBtcOpenOrder o) {
        return new HyperliquidOpenOrderDto(
                o.getOrderId(), o.getOrderTime(), o.getCoin(), o.getSide(), o.getOrderType(),
                o.getPrice(), o.getSize(), o.getOriginalSize(), o.isReduceOnly());
    }
}

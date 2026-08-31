package com.mamta.btctrade.autotradebtc.whale;

import com.mamta.btctrade.autotradebtc.hyperliquid.HyperliquidClient;
import com.mamta.btctrade.autotradebtc.hyperliquid.HyperliquidLeaderboardClient;
import com.mamta.btctrade.autotradebtc.hyperliquid.dto.HyperliquidPositionDto;
import com.mamta.btctrade.autotradebtc.hyperliquid.dto.LeaderboardEntry;
import com.mamta.btctrade.autotradebtc.wallet.Chain;
import com.mamta.btctrade.autotradebtc.wallet.WalletService;
import com.mamta.btctrade.autotradebtc.wallet.WalletSource;
import com.mamta.btctrade.autotradebtc.wallet.dto.WalletResponse;
import com.mamta.btctrade.autotradebtc.whale.dto.WalletAutoMarkResult;
import com.mamta.btctrade.autotradebtc.whale.dto.WhalePositionResponse;
import com.mamta.btctrade.autotradebtc.whale.dto.WhalePositionSummary;
import com.mamta.btctrade.autotradebtc.whale.dto.WhalePositionsDashboardResponse;
import com.mamta.btctrade.autotradebtc.whale.dto.WhaleScanResult;
import com.mamta.btctrade.autotradebtc.whale.dto.WhaleScanStatus;
import com.mamta.btctrade.autotradebtc.whale.dto.WhaleWalletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Periodically scans Hyperliquid's leaderboard for whale wallets that are consistently
 * profitable and trading under a leverage cap, auto-saving any newly found ones.
 *
 * "Consistently profitable" is approximated as positive PnL in both the leaderboard's
 * "month" and "allTime" windows - Hyperliquid's public leaderboard has no native 3-month
 * bucket, so this is the closest available signal without maintaining our own long-running
 * history of snapshots.
 */
@Service
public class WhaleScanService {

    private static final Logger log = LoggerFactory.getLogger(WhaleScanService.class);

    private final HyperliquidLeaderboardClient leaderboardClient;
    private final HyperliquidClient hyperliquidClient;
    private final WalletService walletService;
    private final BigDecimal minAccountValue;
    private final BigDecimal maxLeverage;
    private final BigDecimal autoMarkMinTotalPnl;

    private volatile Instant lastScanAt;
    private volatile int lastCandidateCount;
    private volatile int lastDiscoveredCount;
    private volatile Map<String, LeaderboardEntry> lastLeaderboardByAddress = Map.of();

    public WhaleScanService(
            HyperliquidLeaderboardClient leaderboardClient,
            HyperliquidClient hyperliquidClient,
            WalletService walletService,
            @Value("${app.whale.min-account-value}") BigDecimal minAccountValue,
            @Value("${app.whale.max-leverage}") BigDecimal maxLeverage,
            @Value("${app.whale.auto-mark-min-total-pnl:10000000}") BigDecimal autoMarkMinTotalPnl) {
        this.leaderboardClient = leaderboardClient;
        this.hyperliquidClient = hyperliquidClient;
        this.walletService = walletService;
        this.minAccountValue = minAccountValue;
        this.maxLeverage = maxLeverage;
        this.autoMarkMinTotalPnl = autoMarkMinTotalPnl;
    }

    @Scheduled(fixedRateString = "${app.whale.scan-interval-ms:180000}")
    public void scheduledScan() {
        scanNow();
    }

    public WhaleScanResult scanNow() {
        List<LeaderboardEntry> entries = leaderboardClient.fetchLeaderboard();

        List<LeaderboardEntry> whaleCandidates = entries.stream()
                .filter(e -> e.accountValue() != null && e.accountValue().compareTo(minAccountValue) >= 0)
                .filter(this::isConsistentlyProfitable)
                .toList();

        int discovered = 0;
        for (LeaderboardEntry candidate : whaleCandidates) {
            BigDecimal leverage = hyperliquidClient.fetchMaxLeverage(candidate.address());
            if (leverage == null || leverage.compareTo(maxLeverage) >= 0) {
                continue;
            }
            String label = "Auto: whale, <%sx leverage, profitable".formatted(maxLeverage.stripTrailingZeros().toPlainString());
            boolean added = walletService.saveIfAbsent(candidate.address(), Chain.HYPERLIQUID, label);
            if (added) {
                discovered++;
                log.info("Auto-tracked whale wallet {} (leverage {}x)", candidate.address(), leverage);
            }
        }

        lastScanAt = Instant.now();
        lastCandidateCount = whaleCandidates.size();
        lastDiscoveredCount = discovered;
        lastLeaderboardByAddress = entries.stream()
                .collect(Collectors.toMap(
                        e -> e.address().toLowerCase(),
                        Function.identity(),
                        (first, second) -> first));
        return new WhaleScanResult(lastScanAt, entries.size(), whaleCandidates.size(), discovered);
    }

    public WhaleScanStatus status() {
        return new WhaleScanStatus(lastScanAt, lastCandidateCount, lastDiscoveredCount, minAccountValue, maxLeverage);
    }

    /**
     * Tracked whale wallets (auto-discovered via the leaderboard scan), enriched with the PnL
     * figures from the most recent scan that observed each address. PnL fields are null for a
     * wallet not seen in the latest leaderboard snapshot yet.
     */
    public List<WhaleWalletResponse> listWhaleWallets() {
        Map<String, LeaderboardEntry> snapshot = lastLeaderboardByAddress;
        List<WalletResponse> whaleWallets = walletService.listWallets(WalletSource.AUTO_WHALE, Chain.HYPERLIQUID);
        return whaleWallets.stream()
                .map(w -> {
                    LeaderboardEntry entry = snapshot.get(w.address().toLowerCase());
                    return new WhaleWalletResponse(
                            w.id(),
                            w.address(),
                            w.label(),
                            w.createdAt(),
                            entry != null ? entry.accountValue() : null,
                            entry != null ? entry.monthPnl() : null,
                            entry != null ? entry.allTimePnl() : null);
                })
                .toList();
    }

    /**
     * Long/short split of tracked whales' currently open BTC positions on Hyperliquid, fetched
     * live (one {@code clearinghouseState} call per tracked whale) rather than from the last
     * leaderboard scan, since positions can change between scans.
     */
    public WhalePositionsDashboardResponse getBtcPositionDashboard() {
        List<WalletResponse> whaleWallets = walletService.listWallets(WalletSource.AUTO_WHALE, Chain.HYPERLIQUID);

        List<WhalePositionResponse> positions = new ArrayList<>();
        for (WalletResponse wallet : whaleWallets) {
            hyperliquidClient.fetchPositions(wallet.address()).stream()
                    .filter(p -> "BTC".equals(p.coin()))
                    .findFirst()
                    .ifPresent(p -> positions.add(new WhalePositionResponse(
                            wallet.id(),
                            wallet.address(),
                            wallet.label(),
                            p.side(),
                            p.size(),
                            p.positionValue(),
                            p.entryPrice(),
                            p.unrealizedPnl(),
                            p.leverage())));
        }

        return new WhalePositionsDashboardResponse(summarize(positions), positions);
    }

    private static WhalePositionSummary summarize(List<WhalePositionResponse> positions) {
        int longCount = 0;
        int shortCount = 0;
        BigDecimal longSize = BigDecimal.ZERO;
        BigDecimal shortSize = BigDecimal.ZERO;
        BigDecimal longNotional = BigDecimal.ZERO;
        BigDecimal shortNotional = BigDecimal.ZERO;
        for (WhalePositionResponse p : positions) {
            BigDecimal notional = p.notionalUsd() != null ? p.notionalUsd() : BigDecimal.ZERO;
            if ("LONG".equals(p.side())) {
                longCount++;
                longSize = longSize.add(p.size());
                longNotional = longNotional.add(notional);
            } else {
                shortCount++;
                shortSize = shortSize.add(p.size());
                shortNotional = shortNotional.add(notional);
            }
        }

        BigDecimal totalSize = longSize.add(shortSize);
        String majoritySide = null;
        BigDecimal majoritySharePct = null;
        if (totalSize.signum() > 0) {
            majoritySide = longSize.compareTo(shortSize) >= 0 ? "LONG" : "SHORT";
            BigDecimal majoritySize = "LONG".equals(majoritySide) ? longSize : shortSize;
            majoritySharePct = majoritySize.multiply(BigDecimal.valueOf(100))
                    .divide(totalSize, 2, RoundingMode.HALF_UP);
        }

        return new WhalePositionSummary(
                longCount, shortCount, longSize, shortSize, longNotional, shortNotional,
                majoritySide, majoritySharePct);
    }

    /**
     * Checks every tracked (non-deleted) Hyperliquid wallet against a fresh leaderboard snapshot
     * and marks any wallet whose all-time PnL exceeds {@code autoMarkMinTotalPnl} and isn't
     * already marked - a one-click stand-in for opening each wallet's Coinglass page and marking
     * the large ones by hand. A wallet absent from the current leaderboard snapshot (e.g. it fell
     * off the top ranks, or was never on it) can't be assessed and is skipped, not marked.
     */
    public WalletAutoMarkResult autoMarkByTotalPnl() {
        List<LeaderboardEntry> entries = leaderboardClient.fetchLeaderboard();
        Map<String, LeaderboardEntry> byAddress = entries.stream()
                .collect(Collectors.toMap(
                        e -> e.address().toLowerCase(),
                        Function.identity(),
                        (first, second) -> first));

        List<WalletResponse> hyperliquidWallets = walletService.listWallets().stream()
                .filter(w -> w.chain() == Chain.HYPERLIQUID)
                .toList();

        int newlyMarked = 0;
        int skippedNoData = 0;
        for (WalletResponse wallet : hyperliquidWallets) {
            LeaderboardEntry entry = byAddress.get(wallet.address().toLowerCase());
            if (entry == null || entry.allTimePnl() == null) {
                skippedNoData++;
                continue;
            }
            if (!wallet.marked() && entry.allTimePnl().compareTo(autoMarkMinTotalPnl) > 0) {
                walletService.setMarked(wallet.id(), true);
                newlyMarked++;
            }
        }

        return new WalletAutoMarkResult(hyperliquidWallets.size(), newlyMarked, skippedNoData, autoMarkMinTotalPnl);
    }

    private boolean isConsistentlyProfitable(LeaderboardEntry entry) {
        return entry.monthPnl() != null && entry.monthPnl().signum() > 0
                && entry.allTimePnl() != null && entry.allTimePnl().signum() > 0;
    }
}

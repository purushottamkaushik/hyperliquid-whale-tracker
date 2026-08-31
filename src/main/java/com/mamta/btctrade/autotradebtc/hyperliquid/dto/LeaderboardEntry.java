package com.mamta.btctrade.autotradebtc.hyperliquid.dto;

import java.math.BigDecimal;

/**
 * A trader entry from Hyperliquid's public leaderboard, with the two PnL windows used
 * as a "consistently profitable" proxy (see {@code HyperliquidLeaderboardClient}).
 */
public record LeaderboardEntry(
        String address,
        BigDecimal accountValue,
        BigDecimal monthPnl,
        BigDecimal allTimePnl
) {
}

package com.mamta.btctrade.autotradebtc.whale.dto;

import java.math.BigDecimal;

/**
 * Result of a manual "scan and mark" pass: every tracked Hyperliquid wallet is checked against
 * its latest Hyperliquid leaderboard all-time PnL, and any wallet over {@code minTotalPnl} that
 * wasn't already marked gets marked. Replaces the manual workflow of opening each wallet's
 * Coinglass page and marking the large ones by hand.
 */
public record WalletAutoMarkResult(
        int walletsChecked,
        int newlyMarked,
        int skippedNoLeaderboardData,
        BigDecimal minTotalPnl
) {
}

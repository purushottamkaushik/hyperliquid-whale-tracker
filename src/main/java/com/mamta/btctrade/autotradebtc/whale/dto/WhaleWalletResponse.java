package com.mamta.btctrade.autotradebtc.whale.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A tracked whale wallet enriched with the PnL figures from the most recent leaderboard scan
 * that discovered/last saw it. PnL fields are null until a scan has observed this address.
 */
public record WhaleWalletResponse(
        Long id,
        String address,
        String label,
        Instant createdAt,
        BigDecimal accountValue,
        BigDecimal monthPnl,
        BigDecimal allTimePnl
) {
}

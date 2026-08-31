package com.mamta.btctrade.autotradebtc.whale.dto;

import java.math.BigDecimal;

/**
 * Aggregate long/short split across tracked whales' open BTC positions on Hyperliquid.
 * {@code majoritySide} and {@code majorityBtcSharePct} are null when no whale currently
 * holds a BTC position.
 */
public record WhalePositionSummary(
        int longCount,
        int shortCount,
        BigDecimal longBtcSize,
        BigDecimal shortBtcSize,
        BigDecimal longNotionalUsd,
        BigDecimal shortNotionalUsd,
        String majoritySide,
        BigDecimal majorityBtcSharePct
) {
}

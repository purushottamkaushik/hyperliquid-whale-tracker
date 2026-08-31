package com.mamta.btctrade.autotradebtc.whale.dto;

import java.math.BigDecimal;

/**
 * A tracked whale's currently open BTC position on Hyperliquid.
 */
public record WhalePositionResponse(
        Long walletId,
        String address,
        String label,
        String side,
        BigDecimal size,
        BigDecimal notionalUsd,
        BigDecimal entryPrice,
        BigDecimal unrealizedPnl,
        BigDecimal leverage
) {
}

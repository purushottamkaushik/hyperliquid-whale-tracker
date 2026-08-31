package com.mamta.btctrade.autotradebtc.hyperliquid.dto;

import java.math.BigDecimal;

/**
 * One open position from Hyperliquid's {@code clearinghouseState} info response.
 * {@code size} is always positive - direction is carried by {@code side}.
 */
public record HyperliquidPositionDto(
        String coin,
        String side,
        BigDecimal size,
        BigDecimal entryPrice,
        BigDecimal positionValue,
        BigDecimal unrealizedPnl,
        BigDecimal leverage
) {
}

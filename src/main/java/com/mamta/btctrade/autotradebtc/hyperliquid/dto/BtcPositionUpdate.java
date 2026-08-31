package com.mamta.btctrade.autotradebtc.hyperliquid.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A marked wallet's current BTC position, pushed live from Hyperliquid's per-user
 * "clearinghouseState" WebSocket subscription. {@code side}/{@code size}/etc. are null when the
 * wallet currently has no open BTC position (it still gets an event so a closed position clears
 * the UI promptly).
 */
public record BtcPositionUpdate(
        String address,
        Instant time,
        String side,
        BigDecimal size,
        BigDecimal entryPrice,
        BigDecimal positionValue,
        BigDecimal unrealizedPnl,
        BigDecimal leverage
) {
}

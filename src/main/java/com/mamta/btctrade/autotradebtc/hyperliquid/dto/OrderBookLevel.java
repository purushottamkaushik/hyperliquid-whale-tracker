package com.mamta.btctrade.autotradebtc.hyperliquid.dto;

import java.math.BigDecimal;

/** One price level in a Hyperliquid order book - aggregated across every resting order at that price. */
public record OrderBookLevel(
        BigDecimal price,
        BigDecimal size,
        int orderCount
) {
}

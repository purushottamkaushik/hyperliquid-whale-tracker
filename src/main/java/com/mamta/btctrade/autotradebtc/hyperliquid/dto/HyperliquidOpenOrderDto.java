package com.mamta.btctrade.autotradebtc.hyperliquid.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record HyperliquidOpenOrderDto(
        long orderId,
        Instant time,
        String coin,
        String side,
        String orderType,
        BigDecimal price,
        BigDecimal size,
        BigDecimal originalSize,
        boolean reduceOnly
) {
}

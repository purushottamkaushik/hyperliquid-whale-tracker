package com.mamta.btctrade.autotradebtc.hyperliquid.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record HyperliquidFillDto(
        String txid,
        Instant time,
        String coin,
        String side,
        String direction,
        BigDecimal price,
        BigDecimal size,
        BigDecimal fee,
        String feeToken,
        BigDecimal closedPnl
) {
}

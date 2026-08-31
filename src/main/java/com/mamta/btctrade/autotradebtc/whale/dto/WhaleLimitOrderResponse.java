package com.mamta.btctrade.autotradebtc.whale.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record WhaleLimitOrderResponse(
        Long id,
        String exchange,
        String symbol,
        String side,
        BigDecimal price,
        BigDecimal size,
        BigDecimal notionalUsd,
        String status,
        Instant firstSeenAt,
        Instant lastSeenAt,
        Instant removedAt
) {
}

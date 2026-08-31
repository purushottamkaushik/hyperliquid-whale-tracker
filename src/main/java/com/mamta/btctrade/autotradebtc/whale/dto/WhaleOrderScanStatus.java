package com.mamta.btctrade.autotradebtc.whale.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record WhaleOrderScanStatus(
        Instant lastScanAt,
        BigDecimal minNotionalUsd,
        long activeCount,
        Map<String, Long> activeCountByExchange,
        Map<String, String> exchangeErrors
) {
}

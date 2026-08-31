package com.mamta.btctrade.autotradebtc.whale.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record WhaleScanStatus(
        Instant lastScanAt,
        int lastCandidateCount,
        int lastDiscoveredCount,
        BigDecimal minAccountValue,
        BigDecimal maxLeverage
) {
}

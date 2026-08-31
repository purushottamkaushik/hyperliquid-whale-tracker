package com.mamta.btctrade.autotradebtc.whale.dto;

import java.time.Instant;

public record WhaleScanResult(
        Instant scannedAt,
        int totalLeaderboardEntries,
        int whaleCandidates,
        int newlyDiscovered
) {
}

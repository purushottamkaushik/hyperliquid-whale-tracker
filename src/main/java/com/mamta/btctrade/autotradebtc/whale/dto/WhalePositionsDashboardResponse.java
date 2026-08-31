package com.mamta.btctrade.autotradebtc.whale.dto;

import java.util.List;

public record WhalePositionsDashboardResponse(
        WhalePositionSummary summary,
        List<WhalePositionResponse> positions
) {
}

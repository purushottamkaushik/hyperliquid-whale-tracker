package com.mamta.btctrade.autotradebtc.hyperliquid.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Raw shape of an entry in Hyperliquid's {@code userFills} info response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HyperliquidFill(
        String coin,
        String px,
        String sz,
        String side,
        long time,
        String dir,
        String closedPnl,
        String hash,
        String fee,
        String feeToken
) {
}

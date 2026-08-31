package com.mamta.btctrade.autotradebtc.hyperliquid.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Raw shape of an entry in Hyperliquid's {@code frontendOpenOrders} info response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HyperliquidOpenOrder(
        String coin,
        String side,
        String limitPx,
        String sz,
        String origSz,
        long oid,
        long timestamp,
        String orderType,
        boolean reduceOnly
) {
}

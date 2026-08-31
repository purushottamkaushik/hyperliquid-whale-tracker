package com.mamta.btctrade.autotradebtc.hyperliquid.dto;

import java.time.Instant;
import java.util.List;

/**
 * Full order book depth snapshot for one coin - every push from Hyperliquid's "l2Book"
 * subscription is a complete replacement snapshot, not a delta, so consumers should just render
 * the latest one rather than merge it with a prior version.
 */
public record OrderBookSnapshot(
        String coin,
        Instant time,
        List<OrderBookLevel> bids,
        List<OrderBookLevel> asks
) {
}

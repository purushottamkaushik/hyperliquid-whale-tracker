package com.mamta.btctrade.autotradebtc.exchange;

import java.time.Instant;
import java.util.List;

/** A one-shot BTC order book snapshot from a single exchange. */
public record ExchangeOrderBookSnapshot(
        ExchangeName exchange,
        Instant time,
        List<ExchangeOrderBookLevel> bids,
        List<ExchangeOrderBookLevel> asks
) {
}

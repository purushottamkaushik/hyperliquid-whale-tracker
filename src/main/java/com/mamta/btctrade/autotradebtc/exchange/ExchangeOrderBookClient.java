package com.mamta.btctrade.autotradebtc.exchange;

/** Fetches a BTC order book snapshot from one exchange's public depth API. */
public interface ExchangeOrderBookClient {

    ExchangeName exchange();

    /** Latest order book snapshot, or null if the fetch failed. */
    ExchangeOrderBookSnapshot fetchBtcOrderBook();
}

package com.mamta.btctrade.autotradebtc.hyperliquid.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One executed trade on Hyperliquid's BTC perpetual market, from the public "trades" WebSocket
 * feed - market-wide, every trade by every trader, not just tracked wallets. "taker" is whichever
 * side's order triggered the match; "maker" is the resting order it matched against. {@code side}
 * is the taker's side ("BUY" if the taker bought, "SELL" if the taker sold) - the conventional way
 * a trade tape is read.
 */
public record BtcTradeEvent(
        long tradeId,
        Instant time,
        String side,
        BigDecimal price,
        BigDecimal size,
        BigDecimal notionalUsd,
        String takerAddress,
        String makerAddress
) {
}

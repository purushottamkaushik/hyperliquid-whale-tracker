package com.mamta.btctrade.autotradebtc.exchange;

import java.math.BigDecimal;

/** One price level in an exchange's BTC order book - size is denominated directly in BTC. */
public record ExchangeOrderBookLevel(BigDecimal price, BigDecimal size) {
}

package com.mamta.btctrade.autotradebtc.exchange;

import com.mamta.btctrade.autotradebtc.hyperliquid.HyperliquidOrderBookStreamClient;
import com.mamta.btctrade.autotradebtc.hyperliquid.dto.OrderBookSnapshot;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adapts Hyperliquid's already-streamed BTC perpetual order book (kept live by
 * {@link HyperliquidOrderBookStreamClient}) to the common {@link ExchangeOrderBookClient} shape,
 * so the whale-order scanner can treat it the same as the polled spot exchanges - no extra
 * polling needed since the WebSocket connection is already maintained for the live trades page.
 */
@Component
public class HyperliquidOrderBookAdapter implements ExchangeOrderBookClient {

    private final HyperliquidOrderBookStreamClient streamClient;

    public HyperliquidOrderBookAdapter(HyperliquidOrderBookStreamClient streamClient) {
        this.streamClient = streamClient;
    }

    @Override
    public ExchangeName exchange() {
        return ExchangeName.HYPERLIQUID;
    }

    @Override
    public ExchangeOrderBookSnapshot fetchBtcOrderBook() {
        OrderBookSnapshot snapshot = streamClient.getLatest();
        if (snapshot == null) {
            return null;
        }
        return new ExchangeOrderBookSnapshot(
                ExchangeName.HYPERLIQUID,
                snapshot.time(),
                toLevels(snapshot.bids()),
                toLevels(snapshot.asks()));
    }

    private static List<ExchangeOrderBookLevel> toLevels(
            List<com.mamta.btctrade.autotradebtc.hyperliquid.dto.OrderBookLevel> levels) {
        return levels.stream()
                .map(l -> new ExchangeOrderBookLevel(l.price(), l.size()))
                .toList();
    }
}

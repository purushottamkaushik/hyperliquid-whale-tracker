package com.mamta.btctrade.autotradebtc.whale;

import com.mamta.btctrade.autotradebtc.hyperliquid.HyperliquidOrderBookStreamClient;
import com.mamta.btctrade.autotradebtc.hyperliquid.dto.OrderBookSnapshot;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Real-time BTC order book depth on Hyperliquid - market-wide, sourced from
 * {@link HyperliquidOrderBookStreamClient}'s persistent WebSocket connection.
 */
@RestController
@RequestMapping("/api/btc-orderbook")
public class OrderBookController {

    private final HyperliquidOrderBookStreamClient orderBookStreamClient;

    public OrderBookController(HyperliquidOrderBookStreamClient orderBookStreamClient) {
        this.orderBookStreamClient = orderBookStreamClient;
    }

    /** Latest order book snapshot, or null if the stream hasn't received one yet. */
    @GetMapping("/snapshot")
    public OrderBookSnapshot snapshot() {
        return orderBookStreamClient.getLatest();
    }

    /** Server-Sent Events stream pushing the full order book snapshot on every update. */
    @GetMapping("/stream")
    public SseEmitter stream() {
        return orderBookStreamClient.subscribe();
    }
}

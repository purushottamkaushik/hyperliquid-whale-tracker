package com.mamta.btctrade.autotradebtc.whale;

import com.mamta.btctrade.autotradebtc.hyperliquid.HyperliquidTradeStreamClient;
import com.mamta.btctrade.autotradebtc.hyperliquid.dto.BtcTradeEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * Real-time feed of BTC perpetual trades on Hyperliquid - market-wide (every trader, not just
 * tracked wallets), sourced from {@link HyperliquidTradeStreamClient}'s persistent WebSocket
 * connection to Hyperliquid's public trade stream.
 */
@RestController
@RequestMapping("/api/btc-trades")
public class BtcTradeFeedController {

    private final HyperliquidTradeStreamClient tradeStreamClient;

    public BtcTradeFeedController(HyperliquidTradeStreamClient tradeStreamClient) {
        this.tradeStreamClient = tradeStreamClient;
    }

    /** Most recently buffered trades, newest first - the initial snapshot before the live stream catches up. */
    @GetMapping("/recent")
    public List<BtcTradeEvent> recent() {
        return tradeStreamClient.getRecentTrades();
    }

    /** Server-Sent Events stream pushing new trades as they happen. */
    @GetMapping("/stream")
    public SseEmitter stream() {
        return tradeStreamClient.subscribe();
    }
}

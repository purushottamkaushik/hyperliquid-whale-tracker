package com.mamta.btctrade.autotradebtc.wallet;

import com.mamta.btctrade.autotradebtc.hyperliquid.dto.BtcPositionUpdate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * Real-time BTC position feed for marked Hyperliquid wallets, sourced from
 * {@link WalletPositionStreamService}'s live per-wallet WebSocket subscriptions.
 */
@RestController
@RequestMapping("/api/wallet-positions")
public class WalletPositionFeedController {

    private final WalletPositionStreamService positionStreamService;

    public WalletPositionFeedController(WalletPositionStreamService positionStreamService) {
        this.positionStreamService = positionStreamService;
    }

    /** Latest known BTC position for every currently-subscribed marked wallet. */
    @GetMapping("/recent")
    public List<BtcPositionUpdate> recent() {
        return positionStreamService.getSnapshot();
    }

    /** Server-Sent Events stream pushing a position update whenever one changes. */
    @GetMapping("/stream")
    public SseEmitter stream() {
        return positionStreamService.subscribe();
    }
}

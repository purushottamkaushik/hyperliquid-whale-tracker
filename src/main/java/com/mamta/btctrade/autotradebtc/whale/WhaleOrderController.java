package com.mamta.btctrade.autotradebtc.whale;

import com.mamta.btctrade.autotradebtc.whale.dto.WhaleLimitOrderResponse;
import com.mamta.btctrade.autotradebtc.whale.dto.WhaleOrderScanStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Large resting BTC limit orders tracked across exchange order books over time - Coinglass
 * "large orderbook statistics" style (https://www.coinglass.com/large-orderbook-statistics), but
 * self-hosted. See {@link WhaleOrderScanService} for how orders are discovered and their
 * ACTIVE/REMOVED lifecycle tracked.
 */
@RestController
@RequestMapping("/api/whale-orders")
public class WhaleOrderController {

    private final WhaleOrderScanService scanService;

    public WhaleOrderController(WhaleOrderScanService scanService) {
        this.scanService = scanService;
    }

    @GetMapping
    public List<WhaleLimitOrderResponse> list(
            @RequestParam(required = false) String exchange,
            @RequestParam(required = false) String side,
            @RequestParam(name = "includeRemoved", defaultValue = "false") boolean includeRemoved) {
        return scanService.listOrders(exchange, side, includeRemoved);
    }

    @GetMapping("/status")
    public WhaleOrderScanStatus status() {
        return scanService.status();
    }

    /** Runs a scan synchronously (on this request thread, not the shared scheduler) and returns the fresh status. */
    @PostMapping("/scan")
    public WhaleOrderScanStatus scan() {
        scanService.scanNow();
        return scanService.status();
    }
}

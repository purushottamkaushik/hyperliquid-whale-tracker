package com.mamta.btctrade.autotradebtc.whale;

import com.mamta.btctrade.autotradebtc.whale.dto.WalletAutoMarkResult;
import com.mamta.btctrade.autotradebtc.whale.dto.WhalePositionsDashboardResponse;
import com.mamta.btctrade.autotradebtc.whale.dto.WhaleScanResult;
import com.mamta.btctrade.autotradebtc.whale.dto.WhaleScanStatus;
import com.mamta.btctrade.autotradebtc.whale.dto.WhaleWalletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/whales")
public class WhaleController {

    private final WhaleScanService whaleScanService;

    public WhaleController(WhaleScanService whaleScanService) {
        this.whaleScanService = whaleScanService;
    }

    @GetMapping
    public List<WhaleWalletResponse> listWhales() {
        return whaleScanService.listWhaleWallets();
    }

    @PostMapping("/scan")
    public WhaleScanResult scanNow() {
        return whaleScanService.scanNow();
    }

    @GetMapping("/status")
    public WhaleScanStatus status() {
        return whaleScanService.status();
    }

    @GetMapping("/positions")
    public WhalePositionsDashboardResponse btcPositions() {
        return whaleScanService.getBtcPositionDashboard();
    }

    /**
     * Marks every tracked Hyperliquid wallet whose leaderboard all-time PnL exceeds
     * {@code app.whale.auto-mark-min-total-pnl} - the "Scan & mark" button on the wallets screen.
     */
    @PostMapping("/mark-high-pnl")
    public WalletAutoMarkResult markHighPnlWallets() {
        return whaleScanService.autoMarkByTotalPnl();
    }
}

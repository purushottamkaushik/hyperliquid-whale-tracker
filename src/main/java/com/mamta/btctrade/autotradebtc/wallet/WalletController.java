package com.mamta.btctrade.autotradebtc.wallet;

import com.mamta.btctrade.autotradebtc.blockchain.BlockstreamClient;
import com.mamta.btctrade.autotradebtc.hyperliquid.HyperliquidClient;
import com.mamta.btctrade.autotradebtc.hyperliquid.dto.HyperliquidOpenOrderDto;
import com.mamta.btctrade.autotradebtc.hyperliquid.dto.HyperliquidPositionDto;
import com.mamta.btctrade.autotradebtc.wallet.dto.ActiveRequest;
import com.mamta.btctrade.autotradebtc.wallet.dto.MarkedRequest;
import com.mamta.btctrade.autotradebtc.wallet.dto.WalletOverviewResponse;
import com.mamta.btctrade.autotradebtc.wallet.dto.WalletRequest;
import com.mamta.btctrade.autotradebtc.wallet.dto.WalletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/wallets")
public class WalletController {

    private final WalletService walletService;
    private final WalletSyncService walletSyncService;
    private final BlockstreamClient blockstreamClient;
    private final HyperliquidClient hyperliquidClient;

    public WalletController(
            WalletService walletService,
            WalletSyncService walletSyncService,
            BlockstreamClient blockstreamClient,
            HyperliquidClient hyperliquidClient) {
        this.walletService = walletService;
        this.walletSyncService = walletSyncService;
        this.blockstreamClient = blockstreamClient;
        this.hyperliquidClient = hyperliquidClient;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WalletResponse addWallet(@Valid @RequestBody WalletRequest request) {
        return walletService.addWallet(request);
    }

    @GetMapping
    public List<WalletResponse> listWallets() {
        return walletService.listWallets();
    }

    /**
     * Each tracked wallet combined with its latest persisted BTC-only snapshot (total/unrealized
     * PnL, open BTC orders, open BTC position) - refreshed in the background every
     * {@code app.wallet.sync-interval-ms} rather than fetched live on request.
     */
    @GetMapping("/overview")
    public List<WalletOverviewResponse> overview() {
        return walletSyncService.getOverview();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWallet(@PathVariable Long id) {
        walletService.deleteWallet(id);
    }

    /** Toggles the manual "marked" flag - Hyperliquid wallets only. */
    @PatchMapping("/{id}/marked")
    public WalletResponse setMarked(@PathVariable Long id, @RequestBody MarkedRequest request) {
        return walletService.setMarked(id, request.marked());
    }

    /** Soft delete/restore: pauses or resumes tracking without losing the wallet. */
    @PatchMapping("/{id}/active")
    public WalletResponse setActive(@PathVariable Long id, @RequestBody ActiveRequest request) {
        return walletService.setActive(id, request.active());
    }

    @GetMapping("/{id}/transactions")
    public Object getTransactions(@PathVariable Long id) {
        Wallet wallet = walletService.getWalletOrThrow(id);
        return switch (wallet.getChain()) {
            case BTC -> blockstreamClient.fetchTransactions(wallet.getAddress());
            case HYPERLIQUID -> hyperliquidClient.fetchFills(wallet.getAddress());
        };
    }

    @GetMapping("/{id}/open-orders")
    public List<HyperliquidOpenOrderDto> getOpenOrders(@PathVariable Long id) {
        Wallet wallet = walletService.getWalletOrThrow(id);
        if (wallet.getChain() != Chain.HYPERLIQUID) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Open orders are only available for Hyperliquid wallets");
        }
        return hyperliquidClient.fetchOpenOrders(wallet.getAddress());
    }

    @GetMapping("/{id}/positions")
    public List<HyperliquidPositionDto> getPositions(@PathVariable Long id) {
        Wallet wallet = walletService.getWalletOrThrow(id);
        if (wallet.getChain() != Chain.HYPERLIQUID) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Positions are only available for Hyperliquid wallets");
        }
        return hyperliquidClient.fetchPositions(wallet.getAddress());
    }
}

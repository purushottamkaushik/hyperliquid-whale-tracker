package com.mamta.btctrade.autotradebtc.wallet.dto;

import com.mamta.btctrade.autotradebtc.hyperliquid.dto.HyperliquidOpenOrderDto;
import com.mamta.btctrade.autotradebtc.hyperliquid.dto.HyperliquidPositionDto;
import com.mamta.btctrade.autotradebtc.wallet.Chain;
import com.mamta.btctrade.autotradebtc.wallet.WalletSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * A tracked wallet combined with its latest persisted BTC-only trading snapshot (see
 * {@code WalletSyncService}). For BTC-chain wallets, {@code totalPnl}/{@code unrealizedPnl} are
 * null and the lists are empty - positions/orders/PnL are Hyperliquid-only concepts.
 */
public record WalletOverviewResponse(
        Long walletId,
        String address,
        String label,
        Chain chain,
        WalletSource source,
        Instant createdAt,
        boolean marked,
        boolean active,
        BigDecimal totalPnl,
        BigDecimal unrealizedPnl,
        BigDecimal totalPnlBtc,
        BigDecimal unrealizedPnlBtc,
        Instant lastSyncedAt,
        List<HyperliquidOpenOrderDto> openOrders,
        List<HyperliquidPositionDto> positions
) {
}

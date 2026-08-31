package com.mamta.btctrade.autotradebtc.wallet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Latest BTC PnL snapshot for one tracked Hyperliquid wallet, refreshed on
 * {@code app.wallet.sync-interval-ms} by {@link WalletSyncService}. One row per wallet, updated
 * in place (no history kept).
 */
@Entity
@Table(name = "wallet_stats", uniqueConstraints = @UniqueConstraint(columnNames = "wallet_id"))
@Getter
@Setter
@NoArgsConstructor
public class WalletStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wallet_id", nullable = false, unique = true)
    private Long walletId;

    /** Cumulative realized PnL from BTC fills, in USD. */
    @Column(name = "total_pnl", precision = 30, scale = 10)
    private BigDecimal totalPnl;

    /** Unrealized PnL of the wallet's current open BTC position, if any, in USD. */
    @Column(name = "unrealized_pnl", precision = 30, scale = 10)
    private BigDecimal unrealizedPnl;

    /** {@link #totalPnl} converted to BTC at the BTC/USD price observed during this sync. */
    @Column(name = "total_pnl_btc", precision = 30, scale = 10)
    private BigDecimal totalPnlBtc;

    /** {@link #unrealizedPnl} converted to BTC at the BTC/USD price observed during this sync. */
    @Column(name = "unrealized_pnl_btc", precision = 30, scale = 10)
    private BigDecimal unrealizedPnlBtc;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public WalletStat(Long walletId, BigDecimal totalPnl, BigDecimal unrealizedPnl) {
        this.walletId = walletId;
        this.totalPnl = totalPnl;
        this.unrealizedPnl = unrealizedPnl;
        this.updatedAt = Instant.now();
    }
}

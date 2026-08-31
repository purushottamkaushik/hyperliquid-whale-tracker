package com.mamta.btctrade.autotradebtc.wallet;

import com.mamta.btctrade.autotradebtc.hyperliquid.dto.HyperliquidPositionDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A wallet's currently open BTC position on Hyperliquid, as of the last
 * {@link WalletSyncService} run. Replaced (delete + reinsert) on every sync rather than updated,
 * since a wallet has at most one open BTC position at a time.
 */
@Entity
@Table(name = "wallet_btc_positions")
@Getter
@Setter
@NoArgsConstructor
public class WalletBtcPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    @Column(nullable = false, length = 10)
    private String coin;

    @Column(nullable = false, length = 5)
    private String side;

    @Column(precision = 30, scale = 10)
    private BigDecimal size;

    @Column(name = "entry_price", precision = 30, scale = 10)
    private BigDecimal entryPrice;

    @Column(name = "position_value", precision = 30, scale = 10)
    private BigDecimal positionValue;

    @Column(name = "unrealized_pnl", precision = 30, scale = 10)
    private BigDecimal unrealizedPnl;

    @Column(precision = 10, scale = 2)
    private BigDecimal leverage;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public WalletBtcPosition(Long walletId, HyperliquidPositionDto dto) {
        this.walletId = walletId;
        this.coin = dto.coin();
        this.side = dto.side();
        this.size = dto.size();
        this.entryPrice = dto.entryPrice();
        this.positionValue = dto.positionValue();
        this.unrealizedPnl = dto.unrealizedPnl();
        this.leverage = dto.leverage();
        this.updatedAt = Instant.now();
    }
}

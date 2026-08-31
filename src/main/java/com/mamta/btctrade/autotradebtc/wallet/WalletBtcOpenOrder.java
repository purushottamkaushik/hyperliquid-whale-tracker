package com.mamta.btctrade.autotradebtc.wallet;

import com.mamta.btctrade.autotradebtc.hyperliquid.dto.HyperliquidOpenOrderDto;
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
 * One of a wallet's currently open BTC orders on Hyperliquid, as of the last
 * {@link WalletSyncService} run. The whole set is replaced (delete + reinsert) on every sync,
 * since Hyperliquid only reports the live open-order set, not deltas.
 */
@Entity
@Table(name = "wallet_btc_open_orders")
@Getter
@Setter
@NoArgsConstructor
public class WalletBtcOpenOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    @Column(name = "order_id", nullable = false)
    private long orderId;

    @Column(nullable = false, length = 10)
    private String coin;

    @Column(nullable = false, length = 5)
    private String side;

    @Column(name = "order_type", length = 20)
    private String orderType;

    @Column(precision = 30, scale = 10)
    private BigDecimal price;

    @Column(precision = 30, scale = 10)
    private BigDecimal size;

    @Column(name = "original_size", precision = 30, scale = 10)
    private BigDecimal originalSize;

    @Column(name = "reduce_only", nullable = false)
    private boolean reduceOnly;

    @Column(name = "order_time", nullable = false)
    private Instant orderTime;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public WalletBtcOpenOrder(Long walletId, HyperliquidOpenOrderDto dto) {
        this.walletId = walletId;
        this.orderId = dto.orderId();
        this.coin = dto.coin();
        this.side = dto.side();
        this.orderType = dto.orderType();
        this.price = dto.price();
        this.size = dto.size();
        this.originalSize = dto.originalSize();
        this.reduceOnly = dto.reduceOnly();
        this.orderTime = dto.time();
        this.updatedAt = Instant.now();
    }
}

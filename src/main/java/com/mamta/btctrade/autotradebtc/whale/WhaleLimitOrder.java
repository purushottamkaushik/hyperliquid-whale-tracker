package com.mamta.btctrade.autotradebtc.whale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * A large resting BTC limit order - a price level in an exchange's order book whose notional
 * value (price * size) has crossed the configured whale threshold - tracked over time, Coinglass
 * "large orderbook statistics" style. Public depth APIs only expose the aggregated size resting
 * at each price, not individual order ids, so this really tracks "large size resting at a price"
 * rather than a single order; {@code status} flips to REMOVED once {@link WhaleOrderScanService}
 * no longer sees that level holding at or above its hysteresis-lowered threshold.
 */
@Entity
@Table(name = "whale_limit_orders")
@Getter
@Setter
@NoArgsConstructor
public class WhaleLimitOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String exchange;

    @Column(nullable = false, length = 10)
    private String symbol;

    @Column(nullable = false, length = 5)
    private String side;

    @Column(nullable = false, precision = 30, scale = 10)
    private BigDecimal price;

    @Column(nullable = false, precision = 30, scale = 10)
    private BigDecimal size;

    @Column(name = "notional_usd", nullable = false, precision = 30, scale = 2)
    private BigDecimal notionalUsd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private WhaleOrderStatus status;

    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "removed_at")
    private Instant removedAt;

    public WhaleLimitOrder(
            String exchange, String symbol, String side, BigDecimal price, BigDecimal size,
            BigDecimal notionalUsd, Instant now) {
        this.exchange = exchange;
        this.symbol = symbol;
        this.side = side;
        this.price = price;
        this.size = size;
        this.notionalUsd = notionalUsd;
        this.status = WhaleOrderStatus.ACTIVE;
        this.firstSeenAt = now;
        this.lastSeenAt = now;
    }
}

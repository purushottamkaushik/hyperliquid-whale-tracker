package com.mamta.btctrade.autotradebtc.whale;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface WhaleLimitOrderRepository extends JpaRepository<WhaleLimitOrder, Long> {

    List<WhaleLimitOrder> findByExchangeAndSideAndStatus(String exchange, String side, WhaleOrderStatus status);

    List<WhaleLimitOrder> findByStatus(WhaleOrderStatus status);

    List<WhaleLimitOrder> findAllByOrderByNotionalUsdDesc();

    long deleteByStatusAndRemovedAtBefore(WhaleOrderStatus status, Instant cutoff);
}

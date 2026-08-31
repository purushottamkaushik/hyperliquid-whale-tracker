package com.mamta.btctrade.autotradebtc.wallet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface WalletBtcOpenOrderRepository extends JpaRepository<WalletBtcOpenOrder, Long> {

    List<WalletBtcOpenOrder> findByWalletId(Long walletId);

    // Derived delete queries need their own transaction - callers won't otherwise have one open
    // (this repo's callers aren't themselves @Transactional).
    @Transactional
    void deleteByWalletId(Long walletId);
}

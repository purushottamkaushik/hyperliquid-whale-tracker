package com.mamta.btctrade.autotradebtc.wallet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface WalletStatRepository extends JpaRepository<WalletStat, Long> {

    Optional<WalletStat> findByWalletId(Long walletId);

    // Derived delete queries need their own transaction - callers won't otherwise have one open
    // (this repo's callers aren't themselves @Transactional).
    @Transactional
    void deleteByWalletId(Long walletId);
}

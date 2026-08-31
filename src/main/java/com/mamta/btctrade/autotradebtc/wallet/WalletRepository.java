package com.mamta.btctrade.autotradebtc.wallet;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    boolean existsByAddress(String address);

    Optional<Wallet> findByAddress(String address);

    // Excludes hard-deleted wallets (deletedAt set) - use these instead of findAll()/
    // findBySourceAndChain() wherever a hard-deleted wallet must stay invisible everywhere.
    List<Wallet> findByDeletedAtIsNull();

    List<Wallet> findBySourceAndChainAndDeletedAtIsNull(WalletSource source, Chain chain);

    // Next sync batch, oldest-attempted first (nulls - never attempted - sort first in MySQL's
    // ascending order), so every wallet eventually gets a turn instead of a few unlucky ones
    // being retried every cycle at everyone else's expense.
    List<Wallet> findByChainAndDeactivatedAtIsNullAndDeletedAtIsNullOrderByLastSyncAttemptAtAsc(
            Chain chain, Pageable pageable);

    // Marked wallets currently eligible for the live per-wallet WebSocket position feed.
    List<Wallet> findByChainAndMarkedTrueAndDeactivatedAtIsNullAndDeletedAtIsNull(Chain chain);
}

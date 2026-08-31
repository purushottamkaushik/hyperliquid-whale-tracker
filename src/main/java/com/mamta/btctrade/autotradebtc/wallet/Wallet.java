package com.mamta.btctrade.autotradebtc.wallet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "wallets", uniqueConstraints = @UniqueConstraint(columnNames = "address"))
@Getter
@Setter
@NoArgsConstructor
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String address;

    @Column(length = 100)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Chain chain;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WalletSource source;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Manual "favorite"/curation flag - nullable (rather than a primitive boolean) so adding this
     * column to the already-populated {@code wallets} table doesn't require a NOT NULL default;
     * null and false both mean "not marked".
     */
    @Column
    private Boolean marked;

    /**
     * Soft-delete: null means active/tracked; a timestamp means the wallet is paused (hidden from
     * the default view, skipped by the sync job) but can be reactivated. Distinct from a hard
     * delete, which never un-hides.
     */
    @Column
    private Instant deactivatedAt;

    /**
     * Hard-delete marker. The row is kept (not actually removed) rather than deleted outright, so
     * the {@code address} unique constraint keeps blocking the address from ever being re-tracked
     * - by manual add ({@link Chain}/existsByAddress check) or by whale auto-discovery
     * ({@code WalletService#saveIfAbsent}) - without needing a separate exclusion list.
     */
    @Column
    private Instant deletedAt;

    /**
     * When {@code WalletSyncService} last attempted (successfully or not) to sync this wallet -
     * used purely to pick the next batch in round-robin order, never shown to the user (that's
     * {@code WalletStat.updatedAt}, which only advances on a successful sync). Updated regardless
     * of outcome so a wallet that keeps failing (e.g. rate-limited) doesn't get retried every
     * cycle at the expense of every other wallet ever getting a turn.
     */
    @Column
    private Instant lastSyncAttemptAt;

    public Wallet(String address, String label, Chain chain, WalletSource source) {
        this.address = address;
        this.label = label;
        this.chain = chain;
        this.source = source;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
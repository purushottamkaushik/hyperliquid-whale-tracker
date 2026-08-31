package com.mamta.btctrade.autotradebtc.wallet;

import com.mamta.btctrade.autotradebtc.wallet.dto.WalletRequest;
import com.mamta.btctrade.autotradebtc.wallet.dto.WalletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class WalletService {

    // Loose format check (legacy P2PKH, P2SH, and bech32) - not a full base58/bech32 checksum validation.
    private static final Pattern BTC_ADDRESS_PATTERN = Pattern.compile(
            "^([13][a-km-zA-HJ-NP-Z1-9]{25,34}|bc1[a-z0-9]{25,90})$"
    );

    // Hyperliquid accounts use standard 40-hex-char EVM-style addresses.
    private static final Pattern HYPERLIQUID_ADDRESS_PATTERN = Pattern.compile("^0x[a-fA-F0-9]{40}$");

    private final WalletRepository walletRepository;
    private final WalletStatRepository walletStatRepository;
    private final WalletBtcPositionRepository walletBtcPositionRepository;
    private final WalletBtcOpenOrderRepository walletBtcOpenOrderRepository;

    public WalletService(
            WalletRepository walletRepository,
            WalletStatRepository walletStatRepository,
            WalletBtcPositionRepository walletBtcPositionRepository,
            WalletBtcOpenOrderRepository walletBtcOpenOrderRepository) {
        this.walletRepository = walletRepository;
        this.walletStatRepository = walletStatRepository;
        this.walletBtcPositionRepository = walletBtcPositionRepository;
        this.walletBtcOpenOrderRepository = walletBtcOpenOrderRepository;
    }

    public WalletResponse addWallet(WalletRequest request) {
        String rawAddress = request.address().trim();
        Chain chain;
        String address;
        if (BTC_ADDRESS_PATTERN.matcher(rawAddress).matches()) {
            chain = Chain.BTC;
            address = rawAddress;
        } else if (HYPERLIQUID_ADDRESS_PATTERN.matcher(rawAddress).matches()) {
            chain = Chain.HYPERLIQUID;
            address = rawAddress.toLowerCase();
        } else {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Not a valid Bitcoin or Hyperliquid address: " + rawAddress);
        }
        walletRepository.findByAddress(address).ifPresent(existing -> {
            String message = existing.getDeletedAt() != null
                    ? "This address was deleted and can never be tracked again: " + address
                    : "Wallet already saved: " + address;
            throw new ResponseStatusException(HttpStatus.CONFLICT, message);
        });
        String label = request.label() == null ? null : request.label().trim();
        Wallet wallet = walletRepository.save(new Wallet(address, label, chain, WalletSource.MANUAL));
        return toResponse(wallet);
    }

    /**
     * Used by the whale scanner to auto-track a discovered wallet. Unlike {@link #addWallet}, this
     * silently skips (rather than throws) when the address is already saved - which, since a hard
     * delete never removes the row (see {@link Wallet#getDeletedAt()}), also covers "never track
     * this address again" with no separate exclusion list needed.
     *
     * @return true if a new wallet row was inserted
     */
    public boolean saveIfAbsent(String address, Chain chain, String label) {
        String normalized = chain == Chain.HYPERLIQUID ? address.toLowerCase() : address;
        if (walletRepository.existsByAddress(normalized)) {
            return false;
        }
        walletRepository.save(new Wallet(normalized, label, chain, WalletSource.AUTO_WHALE));
        return true;
    }

    public List<WalletResponse> listWallets() {
        return walletRepository.findByDeletedAtIsNull().stream()
                .map(WalletService::toResponse)
                .toList();
    }

    public List<WalletResponse> listWallets(WalletSource source, Chain chain) {
        return walletRepository.findBySourceAndChainAndDeletedAtIsNull(source, chain).stream()
                .map(WalletService::toResponse)
                .toList();
    }

    public Wallet getWalletOrThrow(Long id) {
        return walletRepository.findById(id)
                .filter(w -> w.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found: " + id));
    }

    /**
     * Hard delete: rather than removing the row, marks it deleted-forever and purges its cached
     * BTC sync data. The row stays (so its address keeps failing the uniqueness check elsewhere),
     * meaning this wallet can never be manually re-added or auto-rediscovered by the whale scan.
     */
    public void deleteWallet(Long id) {
        Wallet wallet = getWalletOrThrow(id);
        walletStatRepository.deleteByWalletId(id);
        walletBtcPositionRepository.deleteByWalletId(id);
        walletBtcOpenOrderRepository.deleteByWalletId(id);
        wallet.setDeletedAt(Instant.now());
        walletRepository.save(wallet);
    }

    public WalletResponse setMarked(Long id, boolean marked) {
        Wallet wallet = getWalletOrThrow(id);
        if (wallet.getChain() != Chain.HYPERLIQUID) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only Hyperliquid wallets can be marked");
        }
        wallet.setMarked(marked);
        return toResponse(walletRepository.save(wallet));
    }

    /** Soft delete/restore: pauses (or resumes) tracking without losing the wallet or its history. */
    public WalletResponse setActive(Long id, boolean active) {
        Wallet wallet = getWalletOrThrow(id);
        wallet.setDeactivatedAt(active ? null : Instant.now());
        return toResponse(walletRepository.save(wallet));
    }

    private static WalletResponse toResponse(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(), wallet.getAddress(), wallet.getLabel(), wallet.getChain(),
                wallet.getSource(), wallet.getCreatedAt(),
                Boolean.TRUE.equals(wallet.getMarked()), wallet.getDeactivatedAt() == null);
    }
}

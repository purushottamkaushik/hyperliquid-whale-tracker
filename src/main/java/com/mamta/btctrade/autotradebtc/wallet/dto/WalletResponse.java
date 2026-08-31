package com.mamta.btctrade.autotradebtc.wallet.dto;

import com.mamta.btctrade.autotradebtc.wallet.Chain;
import com.mamta.btctrade.autotradebtc.wallet.WalletSource;

import java.time.Instant;

public record WalletResponse(
        Long id,
        String address,
        String label,
        Chain chain,
        WalletSource source,
        Instant createdAt,
        boolean marked,
        boolean active
) {
}
package com.mamta.btctrade.autotradebtc.wallet.dto;

import jakarta.validation.constraints.NotBlank;

public record WalletRequest(
        @NotBlank(message = "address is required") String address,
        String label
) {
}
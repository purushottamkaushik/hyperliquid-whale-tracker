package com.mamta.btctrade.autotradebtc.blockchain.dto;

import java.time.Instant;

public record TransactionDto(
        String txid,
        boolean confirmed,
        Long blockHeight,
        Instant blockTime,
        long feeSats,
        long netAmountSats,
        String direction
) {
}
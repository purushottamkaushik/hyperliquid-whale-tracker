package com.mamta.btctrade.autotradebtc.blockchain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BlockstreamTx(
        String txid,
        List<Vin> vin,
        List<Vout> vout,
        long fee,
        TxStatus status
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Vin(Output prevout) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Vout(
            @JsonProperty("scriptpubkey_address") String address,
            long value
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Output(
            @JsonProperty("scriptpubkey_address") String address,
            long value
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TxStatus(
            boolean confirmed,
            @JsonProperty("block_height") Long blockHeight,
            @JsonProperty("block_time") Long blockTime
    ) {
    }
}
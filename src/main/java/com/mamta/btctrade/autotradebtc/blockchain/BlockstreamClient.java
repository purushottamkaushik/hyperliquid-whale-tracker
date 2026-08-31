package com.mamta.btctrade.autotradebtc.blockchain;

import com.mamta.btctrade.autotradebtc.blockchain.dto.BlockstreamTx;
import com.mamta.btctrade.autotradebtc.blockchain.dto.TransactionDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Component
public class BlockstreamClient {

    private final RestClient restClient;

    public BlockstreamClient(RestClient blockstreamRestClient) {
        this.restClient = blockstreamRestClient;
    }

    public List<TransactionDto> fetchTransactions(String address) {
        BlockstreamTx[] txs;
        try {
            txs = restClient.get()
                    .uri("/address/{address}/txs", address)
                    .retrieve()
                    .body(BlockstreamTx[].class);
        } catch (RestClientException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Failed to fetch transactions from blockchain explorer", e);
        }
        if (txs == null) {
            return List.of();
        }
        return Arrays.stream(txs)
                .map(tx -> toDto(tx, address))
                .toList();
    }

    private TransactionDto toDto(BlockstreamTx tx, String address) {
        long received = tx.vout().stream()
                .filter(v -> address.equals(v.address()))
                .mapToLong(BlockstreamTx.Vout::value)
                .sum();
        long sent = tx.vin().stream()
                .map(BlockstreamTx.Vin::prevout)
                .filter(prevout -> prevout != null && address.equals(prevout.address()))
                .mapToLong(BlockstreamTx.Output::value)
                .sum();
        long net = received - sent;
        String direction = net > 0 ? "RECEIVED" : (net < 0 ? "SENT" : "SELF");
        Long blockTimeEpoch = tx.status().blockTime();
        Instant blockTime = blockTimeEpoch != null ? Instant.ofEpochSecond(blockTimeEpoch) : null;

        return new TransactionDto(
                tx.txid(),
                tx.status().confirmed(),
                tx.status().blockHeight(),
                blockTime,
                tx.fee(),
                net,
                direction
        );
    }
}

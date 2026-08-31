package com.mamta.btctrade.autotradebtc.exchange;

import tools.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Public BTC/USDT spot order book depth from Bybit - no API key required. */
@Component
public class BybitOrderBookClient implements ExchangeOrderBookClient {

    private static final Logger log = LoggerFactory.getLogger(BybitOrderBookClient.class);

    private final RestClient restClient;

    public BybitOrderBookClient(RestClient bybitRestClient) {
        this.restClient = bybitRestClient;
    }

    @Override
    public ExchangeName exchange() {
        return ExchangeName.BYBIT;
    }

    @Override
    public ExchangeOrderBookSnapshot fetchBtcOrderBook() {
        JsonNode root;
        try {
            root = restClient.get()
                    .uri("/v5/market/orderbook?category=spot&symbol=BTCUSDT&limit=200")
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            log.warn("Failed to fetch Bybit BTC order book: {}", e.getMessage());
            return null;
        }
        if (root == null || root.path("retCode").asInt() != 0) {
            return null;
        }
        JsonNode result = root.path("result");
        return new ExchangeOrderBookSnapshot(
                ExchangeName.BYBIT,
                Instant.now(),
                toLevels(result.path("b")),
                toLevels(result.path("a")));
    }

    private static List<ExchangeOrderBookLevel> toLevels(JsonNode side) {
        List<ExchangeOrderBookLevel> result = new ArrayList<>();
        for (JsonNode level : side) {
            result.add(new ExchangeOrderBookLevel(
                    new BigDecimal(level.get(0).asText()),
                    new BigDecimal(level.get(1).asText())));
        }
        return result;
    }
}

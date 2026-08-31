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

/** Public BTC-USDT spot order book depth from OKX - no API key required. */
@Component
public class OkxOrderBookClient implements ExchangeOrderBookClient {

    private static final Logger log = LoggerFactory.getLogger(OkxOrderBookClient.class);

    private final RestClient restClient;

    public OkxOrderBookClient(RestClient okxRestClient) {
        this.restClient = okxRestClient;
    }

    @Override
    public ExchangeName exchange() {
        return ExchangeName.OKX;
    }

    @Override
    public ExchangeOrderBookSnapshot fetchBtcOrderBook() {
        JsonNode root;
        try {
            root = restClient.get()
                    .uri("/api/v5/market/books?instId=BTC-USDT&sz=400")
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            log.warn("Failed to fetch OKX BTC order book: {}", e.getMessage());
            return null;
        }
        if (root == null) {
            return null;
        }
        JsonNode book = root.path("data").path(0);
        if (book.isMissingNode()) {
            return null;
        }
        return new ExchangeOrderBookSnapshot(
                ExchangeName.OKX,
                Instant.now(),
                toLevels(book.path("bids")),
                toLevels(book.path("asks")));
    }

    /** Each level is [price, size, deprecated, numOrders] - only the first two fields are used. */
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

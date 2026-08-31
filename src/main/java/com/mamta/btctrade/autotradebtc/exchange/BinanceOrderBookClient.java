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

/** Public BTC/USDT spot order book depth from Binance - no API key required. */
@Component
public class BinanceOrderBookClient implements ExchangeOrderBookClient {

    private static final Logger log = LoggerFactory.getLogger(BinanceOrderBookClient.class);

    private final RestClient restClient;

    public BinanceOrderBookClient(RestClient binanceRestClient) {
        this.restClient = binanceRestClient;
    }

    @Override
    public ExchangeName exchange() {
        return ExchangeName.BINANCE;
    }

    @Override
    public ExchangeOrderBookSnapshot fetchBtcOrderBook() {
        JsonNode root;
        try {
            root = restClient.get()
                    .uri("/api/v3/depth?symbol=BTCUSDT&limit=1000")
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            log.warn("Failed to fetch Binance BTC order book: {}", e.getMessage());
            return null;
        }
        if (root == null) {
            return null;
        }
        return new ExchangeOrderBookSnapshot(
                ExchangeName.BINANCE,
                Instant.now(),
                toLevels(root.path("bids")),
                toLevels(root.path("asks")));
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

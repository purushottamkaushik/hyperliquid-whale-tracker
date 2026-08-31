package com.mamta.btctrade.autotradebtc.hyperliquid;

import tools.jackson.databind.JsonNode;
import com.mamta.btctrade.autotradebtc.hyperliquid.dto.HyperliquidFill;
import com.mamta.btctrade.autotradebtc.hyperliquid.dto.HyperliquidFillDto;
import com.mamta.btctrade.autotradebtc.hyperliquid.dto.HyperliquidOpenOrder;
import com.mamta.btctrade.autotradebtc.hyperliquid.dto.HyperliquidOpenOrderDto;
import com.mamta.btctrade.autotradebtc.hyperliquid.dto.HyperliquidPositionDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class HyperliquidClient {

    /**
     * Hyperliquid's public /info endpoint rate-limits hard: a burst of back-to-back requests
     * starts returning 429 after the very first one (confirmed empirically - one request
     * succeeds, then every immediate follow-up fails, while spacing requests a second apart
     * mostly stays clean, though even that spacing alone still draws an occasional 429 under
     * sustained load - hence the retry-with-backoff in {@link #postInfo}). Every wallet
     * sync/whale-scan call goes through this single client bean, so serializing all requests
     * here keeps every caller under that limit without each of them needing to know about it.
     */
    private static final long MIN_REQUEST_INTERVAL_MS = 1500;
    private static final long RATE_LIMIT_BACKOFF_MS = 3000;
    private static final int MAX_ATTEMPTS = 3;

    private final RestClient restClient;
    private final Object rateLimitLock = new Object();
    private long lastRequestAtMillis = 0;

    public HyperliquidClient(RestClient hyperliquidRestClient) {
        this.restClient = hyperliquidRestClient;
    }

    private void throttle() {
        synchronized (rateLimitLock) {
            long waitMs = lastRequestAtMillis + MIN_REQUEST_INTERVAL_MS - System.currentTimeMillis();
            if (waitMs > 0) {
                sleepQuietly(waitMs);
            }
            lastRequestAtMillis = System.currentTimeMillis();
        }
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** POSTs to {@code /info}, throttled and retried with backoff on a 429. */
    private <T> T postInfo(Map<String, ?> requestBody, Class<T> responseType) {
        HttpClientErrorException.TooManyRequests lastRateLimitError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            throttle();
            try {
                return restClient.post()
                        .uri("/info")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestBody)
                        .retrieve()
                        .body(responseType);
            } catch (HttpClientErrorException.TooManyRequests e) {
                lastRateLimitError = e;
                sleepQuietly(RATE_LIMIT_BACKOFF_MS);
            }
        }
        throw lastRateLimitError;
    }

    public List<HyperliquidFillDto> fetchFills(String address) {
        HyperliquidFill[] fills;
        try {
            fills = postInfo(Map.of("type", "userFills", "user", address), HyperliquidFill[].class);
        } catch (RestClientException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Failed to fetch activity from Hyperliquid", e);
        }
        if (fills == null) {
            return List.of();
        }
        return Arrays.stream(fills)
                .map(HyperliquidClient::toDto)
                .toList();
    }

    public List<HyperliquidOpenOrderDto> fetchOpenOrders(String address) {
        HyperliquidOpenOrder[] orders;
        try {
            orders = postInfo(Map.of("type", "frontendOpenOrders", "user", address), HyperliquidOpenOrder[].class);
        } catch (RestClientException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Failed to fetch open orders from Hyperliquid", e);
        }
        if (orders == null) {
            return List.of();
        }
        return Arrays.stream(orders)
                .map(HyperliquidClient::toOpenOrderDto)
                .toList();
    }

    /**
     * Max leverage across a wallet's currently open Hyperliquid positions, or null if it has none
     * (or the lookup failed) - callers should treat null as "cannot be assessed".
     */
    public BigDecimal fetchMaxLeverage(String address) {
        JsonNode root;
        try {
            root = postInfo(Map.of("type", "clearinghouseState", "user", address), JsonNode.class);
        } catch (RestClientException e) {
            return null;
        }
        if (root == null) {
            return null;
        }
        BigDecimal max = null;
        for (JsonNode assetPosition : root.path("assetPositions")) {
            JsonNode leverageValue = assetPosition.path("position").path("leverage").path("value");
            if (leverageValue.isMissingNode() || leverageValue.isNull()) {
                continue;
            }
            BigDecimal value = BigDecimal.valueOf(leverageValue.asDouble());
            if (max == null || value.compareTo(max) > 0) {
                max = value;
            }
        }
        return max;
    }

    /**
     * Current BTC mid price in USD, from Hyperliquid's {@code allMids} info endpoint (no address
     * needed - it's a single global price feed). Null if the lookup fails or BTC isn't present,
     * matching how {@link #fetchMaxLeverage} treats failures as "cannot be assessed".
     */
    public BigDecimal fetchBtcMidPrice() {
        JsonNode root;
        try {
            root = postInfo(Map.of("type", "allMids"), JsonNode.class);
        } catch (RestClientException e) {
            return null;
        }
        if (root == null) {
            return null;
        }
        return decimalOrNull(root, "BTC");
    }

    /**
     * All of a wallet's currently open Hyperliquid positions (any coin), long/short and size
     * derived from the signed position size ({@code szi}).
     */
    public List<HyperliquidPositionDto> fetchPositions(String address) {
        JsonNode root;
        try {
            root = postInfo(Map.of("type", "clearinghouseState", "user", address), JsonNode.class);
        } catch (RestClientException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Failed to fetch positions from Hyperliquid", e);
        }
        if (root == null) {
            return List.of();
        }
        List<HyperliquidPositionDto> positions = new ArrayList<>();
        for (JsonNode assetPosition : root.path("assetPositions")) {
            JsonNode position = assetPosition.path("position");
            String coin = textOrNull(position, "coin");
            BigDecimal signedSize = decimalOrNull(position, "szi");
            if (coin == null || signedSize == null || signedSize.signum() == 0) {
                continue;
            }
            JsonNode leverageValue = position.path("leverage").path("value");
            BigDecimal leverage = leverageValue.isMissingNode() || leverageValue.isNull()
                    ? null : BigDecimal.valueOf(leverageValue.asDouble());
            positions.add(new HyperliquidPositionDto(
                    coin,
                    signedSize.signum() > 0 ? "LONG" : "SHORT",
                    signedSize.abs(),
                    decimalOrNull(position, "entryPx"),
                    decimalOrNull(position, "positionValue"),
                    decimalOrNull(position, "unrealizedPnl"),
                    leverage));
        }
        return positions;
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private static BigDecimal decimalOrNull(JsonNode node, String field) {
        String text = textOrNull(node, field);
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static HyperliquidOpenOrderDto toOpenOrderDto(HyperliquidOpenOrder order) {
        String side = "B".equals(order.side()) ? "BUY" : "SELL";
        return new HyperliquidOpenOrderDto(
                order.oid(),
                Instant.ofEpochMilli(order.timestamp()),
                order.coin(),
                side,
                order.orderType(),
                new BigDecimal(order.limitPx()),
                new BigDecimal(order.sz()),
                new BigDecimal(order.origSz()),
                order.reduceOnly()
        );
    }

    private static HyperliquidFillDto toDto(HyperliquidFill fill) {
        String side = "B".equals(fill.side()) ? "BUY" : "SELL";
        return new HyperliquidFillDto(
                fill.hash(),
                Instant.ofEpochMilli(fill.time()),
                fill.coin(),
                side,
                fill.dir(),
                new BigDecimal(fill.px()),
                new BigDecimal(fill.sz()),
                fill.fee() != null ? new BigDecimal(fill.fee()) : BigDecimal.ZERO,
                fill.feeToken(),
                fill.closedPnl() != null ? new BigDecimal(fill.closedPnl()) : BigDecimal.ZERO
        );
    }
}

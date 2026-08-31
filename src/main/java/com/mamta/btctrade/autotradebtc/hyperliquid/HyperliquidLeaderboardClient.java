package com.mamta.btctrade.autotradebtc.hyperliquid;

import tools.jackson.databind.JsonNode;
import com.mamta.btctrade.autotradebtc.hyperliquid.dto.LeaderboardEntry;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Talks to Hyperliquid's public stats/leaderboard API - the same one their own web UI uses.
 * This is not an officially documented endpoint, so its shape could change without notice.
 */
@Component
public class HyperliquidLeaderboardClient {

    private final RestClient restClient;

    public HyperliquidLeaderboardClient(RestClient hyperliquidStatsRestClient) {
        this.restClient = hyperliquidStatsRestClient;
    }

    public List<LeaderboardEntry> fetchLeaderboard() {
        JsonNode root;
        try {
            root = restClient.get()
                    .uri("/Mainnet/leaderboard")
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to fetch Hyperliquid leaderboard", e);
        }
        if (root == null) {
            return List.of();
        }

        List<LeaderboardEntry> entries = new ArrayList<>();
        for (JsonNode row : root.path("leaderboardRows")) {
            String address = textOrNull(row, "ethAddress");
            if (address == null) {
                continue;
            }
            entries.add(new LeaderboardEntry(
                    address,
                    decimalOrNull(row, "accountValue"),
                    windowPnl(row, "month"),
                    windowPnl(row, "allTime")
            ));
        }
        return entries;
    }

    private static BigDecimal windowPnl(JsonNode row, String windowName) {
        for (JsonNode pair : row.path("windowPerformances")) {
            if (pair.isArray() && pair.size() == 2 && windowName.equals(pair.get(0).asText())) {
                return decimalOrNull(pair.get(1), "pnl");
            }
        }
        return null;
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
}

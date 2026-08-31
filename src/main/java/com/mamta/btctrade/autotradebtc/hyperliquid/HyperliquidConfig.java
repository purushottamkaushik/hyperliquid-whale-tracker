package com.mamta.btctrade.autotradebtc.hyperliquid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class HyperliquidConfig {

    @Bean
    public RestClient hyperliquidRestClient(@Value("${app.hyperliquid.api.base-url}") String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Bean
    public RestClient hyperliquidStatsRestClient(@Value("${app.hyperliquid.stats-api.base-url}") String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}

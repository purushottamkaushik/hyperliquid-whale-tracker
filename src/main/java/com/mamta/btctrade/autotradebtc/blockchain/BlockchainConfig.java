package com.mamta.btctrade.autotradebtc.blockchain;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class BlockchainConfig {

    @Bean
    public RestClient blockstreamRestClient(@Value("${app.blockchain.explorer.base-url}") String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}

package com.mamta.btctrade.autotradebtc.exchange;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class ExchangeConfig {

    /**
     * Connect/read timeouts for every exchange REST client below - without these, a single
     * unreachable or slow exchange (observed with OKX from some networks) would hang the
     * scheduled whale-order scan indefinitely instead of just failing that one exchange's poll.
     */
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 8000;

    @Bean
    public RestClient binanceRestClient(@Value("${app.exchange.binance.api.base-url}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).requestFactory(timeoutRequestFactory()).build();
    }

    @Bean
    public RestClient okxRestClient(@Value("${app.exchange.okx.api.base-url}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).requestFactory(timeoutRequestFactory()).build();
    }

    @Bean
    public RestClient bybitRestClient(@Value("${app.exchange.bybit.api.base-url}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).requestFactory(timeoutRequestFactory()).build();
    }

    private static ClientHttpRequestFactory timeoutRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        return factory;
    }
}

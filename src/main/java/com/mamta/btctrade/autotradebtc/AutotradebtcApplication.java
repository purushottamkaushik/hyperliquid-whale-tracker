package com.mamta.btctrade.autotradebtc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AutotradebtcApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutotradebtcApplication.class, args);
    }

}

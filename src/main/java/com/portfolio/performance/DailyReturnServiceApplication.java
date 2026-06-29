package com.portfolio.performance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the daily-return-service.
 *
 * <p>This service exposes a single capability: calculating a portfolio's daily return
 * summary and deciding whether the result falls within acceptable tolerance.
 */
@SpringBootApplication
public class DailyReturnServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DailyReturnServiceApplication.class, args);
    }
}

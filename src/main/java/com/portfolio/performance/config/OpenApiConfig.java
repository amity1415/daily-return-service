package com.portfolio.performance.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI metadata shown at the top of the Swagger UI, so the API is self-describing.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI portfolioPerformanceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Portfolio Daily Return API")
                .version("1.0.0")
                .description("""
                        Calculates a portfolio's daily return summary and classifies the result as \
                        VALID, REVIEW_REQUIRED, or INVALID_INPUT.

                        Given a day's begin/end market values, net cash flow, and a benchmark \
                        return, the service computes the portfolio return and excess return, applies \
                        validation and review rules, and returns the decision with explanatory \
                        reasons. Requests are idempotent on portfolioId + valuationDate."""));
    }
}

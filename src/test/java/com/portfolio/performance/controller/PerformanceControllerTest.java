package com.portfolio.performance.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PerformanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void dailyReturn_returns200_forValidRequest() throws Exception {
        String body = """
                {
                  "portfolioId": "PORT-001",
                  "valuationDate": "2026-06-29",
                  "beginMarketValue": 1000000.00,
                  "endMarketValue": 1012500.00,
                  "netCashFlow": 0.00,
                  "benchmarkReturnPct": 1.10,
                  "currency": "USD",
                  "requestedBy": "analyst.jane"
                }
                """;

        mockMvc.perform(post("/api/performance/daily-return")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portfolioId").value("PORT-001"))
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    void dailyReturn_returns400_whenRequiredFieldMissing() throws Exception {
        // Missing portfolioId — structural validation should reject the request.
        String body = """
                {
                  "valuationDate": "2026-06-29",
                  "beginMarketValue": 1000000.00,
                  "endMarketValue": 1012500.00,
                  "netCashFlow": 0.00,
                  "currency": "USD",
                  "requestedBy": "analyst.jane"
                }
                """;

        mockMvc.perform(post("/api/performance/daily-return")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}

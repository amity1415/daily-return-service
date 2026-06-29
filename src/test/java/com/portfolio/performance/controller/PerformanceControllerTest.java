package com.portfolio.performance.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PerformanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void validInput_returns200_withStatusValid() throws Exception {
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
                .andExpect(jsonPath("$.status").value("VALID"))
                .andExpect(jsonPath("$.reasons").isArray())
                .andExpect(jsonPath("$.reasons").isEmpty())
                .andExpect(jsonPath("$.portfolioId").value("PORT-001"))
                .andExpect(jsonPath("$.valuationDate").value("2026-06-29"))
                .andExpect(jsonPath("$.portfolioReturnPct").value(0))
                .andExpect(jsonPath("$.excessReturnPct").value(0))
                .andExpect(jsonPath("$.processedAt").exists());
    }

    @Test
    void negativeMarketValue_returns200_withInvalidInput() throws Exception {
        String body = """
                {
                  "portfolioId": "PORT-001",
                  "valuationDate": "2026-06-29",
                  "beginMarketValue": -1.00,
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
                .andExpect(jsonPath("$.status").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.reasons", hasSize(1)))
                .andExpect(jsonPath("$.reasons[0]", containsString("beginMarketValue")));
    }

    @Test
    void blankCurrency_returns200_withInvalidInput() throws Exception {
        String body = """
                {
                  "portfolioId": "PORT-001",
                  "valuationDate": "2026-06-29",
                  "beginMarketValue": 1000000.00,
                  "endMarketValue": 1012500.00,
                  "netCashFlow": 0.00,
                  "benchmarkReturnPct": 1.10,
                  "currency": "   ",
                  "requestedBy": "analyst.jane"
                }
                """;

        mockMvc.perform(post("/api/performance/daily-return")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.reasons[0]", containsString("currency")));
    }

    @Test
    void malformedJson_returns200_withInvalidInput() throws Exception {
        String body = "{ this is not valid json ";

        mockMvc.perform(post("/api/performance/daily-return")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.reasons", hasSize(1)))
                .andExpect(jsonPath("$.reasons[0]", containsString("malformed")));
    }

    @Test
    void unparseableDate_returns200_withInvalidInput() throws Exception {
        String body = """
                {
                  "portfolioId": "PORT-001",
                  "valuationDate": "not-a-date",
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
                .andExpect(jsonPath("$.status").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.reasons[0]", containsString("parsed")));
    }

    @Test
    void zeroBeginWithNonZeroEnd_returns200_withInvalidInput() throws Exception {
        String body = """
                {
                  "portfolioId": "PORT-001",
                  "valuationDate": "2026-06-29",
                  "beginMarketValue": 0.00,
                  "endMarketValue": 100.00,
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
                .andExpect(jsonPath("$.status").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.reasons[0]", containsString("beginMarketValue is 0")));
    }
}

package com.portfolio.performance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.performance.repository.DeduplicationStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end acceptance tests for the daily-return endpoint, exercising the API exactly as a client
 * would (full Spring context + MockMvc). Covers the five assessment scenarios: a VALID happy path,
 * both REVIEW_REQUIRED triggers, an INVALID_INPUT case, and idempotency.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DailyReturnApiAcceptanceTest {

    private static final String ENDPOINT = "/api/performance/daily-return";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DeduplicationStore deduplicationStore;

    @BeforeEach
    void resetDeduplicationCache() {
        deduplicationStore.clear();
    }

    @Test
    @DisplayName("Test 1 — happy path returns VALID with calculated figures")
    void happyPath_returnsValid() throws Exception {
        String body = requestJson("ACPT-1", "1000000", "1035000", "10000", "1.8");

        mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALID"))
                .andExpect(jsonPath("$.portfolioReturnPct").value(2.5))
                .andExpect(jsonPath("$.excessReturnPct").value(0.7))
                .andExpect(jsonPath("$.reasons", empty()));
    }

    @Test
    @DisplayName("Test 2 — benchmark drift > 5% returns REVIEW_REQUIRED")
    void benchmarkDrift_returnsReviewRequired() throws Exception {
        // Return = 10%, benchmark = 1% -> drift 9% (> 5%). Cash flow is small, so only drift fires.
        String body = requestJson("ACPT-2", "1000000", "1100000", "0", "1.0");

        mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVIEW_REQUIRED"))
                .andExpect(jsonPath("$.reasons", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("Test 3 — cash flow > 20% of begin returns REVIEW_REQUIRED")
    void cashFlowSpike_returnsReviewRequired() throws Exception {
        // netCashFlow 300k > 20% of 1m (200k). Return = (1.3m - 1m - 0.3m)/1m = 0% -> no drift.
        String body = requestJson("ACPT-3", "1000000", "1300000", "300000", "0");

        mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVIEW_REQUIRED"))
                .andExpect(jsonPath("$.reasons", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("Test 4 — negative market value returns INVALID_INPUT")
    void negativeMarketValue_returnsInvalidInput() throws Exception {
        String body = requestJson("ACPT-4", "-1", "100", "0", "1.0");

        mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.reasons", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("Test 5 — duplicate request returns identical status and portfolioReturnPct")
    void duplicateRequest_isIdempotent() throws Exception {
        String body = requestJson("ACPT-5", "1000000", "1035000", "10000", "1.8");

        JsonNode first = objectMapper.readTree(
                mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(body))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString());

        JsonNode second = objectMapper.readTree(
                mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(body))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString());

        assertThat(second.get("status").asText()).isEqualTo(first.get("status").asText());
        assertThat(second.get("portfolioReturnPct").decimalValue())
                .isEqualByComparingTo(first.get("portfolioReturnPct").decimalValue());
    }

    private String requestJson(String portfolioId, String begin, String end,
                               String netCashFlow, String benchmark) {
        return """
                {
                  "portfolioId": "%s",
                  "valuationDate": "2026-06-29",
                  "beginMarketValue": %s,
                  "endMarketValue": %s,
                  "netCashFlow": %s,
                  "benchmarkReturnPct": %s,
                  "currency": "USD",
                  "requestedBy": "analyst.jane"
                }
                """.formatted(portfolioId, begin, end, netCashFlow, benchmark);
    }
}

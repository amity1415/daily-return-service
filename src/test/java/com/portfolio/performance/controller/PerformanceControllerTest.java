package com.portfolio.performance.controller;

import com.portfolio.performance.repository.DeduplicationStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Autowired
    private DeduplicationStore deduplicationStore;

    @BeforeEach
    void resetDeduplicationCache() {
        // The store is a singleton shared across test methods; isolate each test.
        deduplicationStore.clear();
    }

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
                .andExpect(jsonPath("$.portfolioReturnPct").value(1.25))
                .andExpect(jsonPath("$.excessReturnPct").value(0.15))
                .andExpect(jsonPath("$.processedAt").exists());
    }

    @Test
    void goalExample_returns200_withCalculatedFigures() throws Exception {
        String body = """
                {
                  "portfolioId": "PORT-001",
                  "valuationDate": "2026-06-29",
                  "beginMarketValue": 1000000,
                  "endMarketValue": 1035000,
                  "netCashFlow": 10000,
                  "benchmarkReturnPct": 1.8,
                  "currency": "USD",
                  "requestedBy": "analyst.jane"
                }
                """;

        mockMvc.perform(post("/api/performance/daily-return")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALID"))
                .andExpect(jsonPath("$.reasons").isEmpty())
                .andExpect(jsonPath("$.portfolioReturnPct").value(2.5))
                .andExpect(jsonPath("$.excessReturnPct").value(0.7))
                .andExpect(jsonPath("$.benchmarkReturnPct").value(1.8));
    }

    @Test
    void reviewRequired_returns200_withReason() throws Exception {
        // Return ~10% vs benchmark 1% -> drift > 5%.
        String body = """
                {
                  "portfolioId": "PORT-001",
                  "valuationDate": "2026-06-29",
                  "beginMarketValue": 1000000,
                  "endMarketValue": 1100000,
                  "netCashFlow": 0,
                  "benchmarkReturnPct": 1.0,
                  "currency": "USD",
                  "requestedBy": "analyst.jane"
                }
                """;

        mockMvc.perform(post("/api/performance/daily-return")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVIEW_REQUIRED"))
                .andExpect(jsonPath("$.reasons", hasSize(1)));
    }

    @Test
    void duplicateRequest_returnsSameResponse_withoutRecomputing() throws Exception {
        String body = """
                {
                  "portfolioId": "PORT-DUP",
                  "valuationDate": "2026-06-29",
                  "beginMarketValue": 1000000,
                  "endMarketValue": 1035000,
                  "netCashFlow": 10000,
                  "benchmarkReturnPct": 1.8,
                  "currency": "USD",
                  "requestedBy": "analyst.jane"
                }
                """;

        MvcResult first = mockMvc.perform(post("/api/performance/daily-return")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult second = mockMvc.perform(post("/api/performance/daily-return")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn();

        String firstBody = first.getResponse().getContentAsString();
        String secondBody = second.getResponse().getContentAsString();

        // Byte-for-byte identical — including processedAt — proves the second call returned the
        // cached response rather than recomputing (a recompute would produce a new timestamp).
        assertThat(secondBody).isEqualTo(firstBody);
    }

    @Test
    void duplicateOfInvalidInput_returnsSameInvalidResponse() throws Exception {
        String body = """
                {
                  "portfolioId": "PORT-DUP-INVALID",
                  "valuationDate": "2026-06-29",
                  "beginMarketValue": -1,
                  "endMarketValue": 100,
                  "netCashFlow": 0,
                  "benchmarkReturnPct": 1.0,
                  "currency": "USD",
                  "requestedBy": "analyst.jane"
                }
                """;

        String firstBody = mockMvc.perform(post("/api/performance/daily-return")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INVALID_INPUT"))
                .andReturn().getResponse().getContentAsString();

        String secondBody = mockMvc.perform(post("/api/performance/daily-return")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INVALID_INPUT"))
                .andReturn().getResponse().getContentAsString();

        assertThat(secondBody).isEqualTo(firstBody);
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

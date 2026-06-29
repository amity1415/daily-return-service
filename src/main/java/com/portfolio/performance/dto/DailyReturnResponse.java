package com.portfolio.performance.dto;

import com.portfolio.performance.domain.ReturnStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Result of processing a daily return request.
 *
 * <p>The percentage fields are placeholders for now ({@code 0}); the actual return calculation is a
 * later step. The {@code status} + {@code reasons} pair always communicates the outcome: an empty
 * {@code reasons} list accompanies a {@code VALID} result, while {@code INVALID_INPUT} carries one
 * or more human-readable explanations.
 *
 * @param portfolioId        echoes the requested portfolio
 * @param valuationDate      echoes the requested valuation date
 * @param portfolioReturnPct the portfolio's daily return, as a percentage (0 until implemented)
 * @param benchmarkReturnPct the benchmark's daily return, as a percentage (echoed from the request)
 * @param excessReturnPct    portfolio return minus benchmark return (0 until implemented)
 * @param status             the decision for this request
 * @param reasons            explanations for the status (empty when VALID)
 * @param processedAt        ISO-8601 timestamp, with timezone offset, of when the result was produced
 */
@Schema(description = "Daily return summary and status decision.")
public record DailyReturnResponse(

        @Schema(example = "PORT-001")
        String portfolioId,

        @Schema(example = "2026-06-29")
        LocalDate valuationDate,

        @Schema(example = "0")
        BigDecimal portfolioReturnPct,

        @Schema(example = "1.10")
        BigDecimal benchmarkReturnPct,

        @Schema(example = "0")
        BigDecimal excessReturnPct,

        @Schema(example = "VALID")
        ReturnStatus status,

        @Schema(example = "[]")
        List<String> reasons,

        @Schema(example = "2026-06-29T12:00:00Z")
        String processedAt
) {
}

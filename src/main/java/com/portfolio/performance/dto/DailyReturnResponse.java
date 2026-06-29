package com.portfolio.performance.dto;

import com.portfolio.performance.domain.ReturnStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Result of a daily return calculation.
 *
 * <p>The numeric percentage fields are populated once the calculation logic is implemented; for now
 * the service returns a placeholder with a status only.
 *
 * @param portfolioId        echoes the requested portfolio
 * @param portfolioReturnPct the portfolio's daily return, as a percentage
 * @param benchmarkReturnPct the benchmark's daily return, as a percentage
 * @param excessReturnPct    portfolio return minus benchmark return, as a percentage
 * @param status             the tolerance decision for this result
 * @param message            human-readable summary of the outcome
 * @param processedAt        ISO-8601 timestamp of when the result was produced
 */
@Schema(description = "Daily return summary and tolerance decision.")
public record DailyReturnResponse(

        @Schema(example = "PORT-001")
        String portfolioId,

        @Schema(example = "1.25")
        BigDecimal portfolioReturnPct,

        @Schema(example = "1.10")
        BigDecimal benchmarkReturnPct,

        @Schema(example = "0.15")
        BigDecimal excessReturnPct,

        @Schema(example = "WITHIN_TOLERANCE")
        ReturnStatus status,

        @Schema(example = "Endpoint reachable. Calculation not yet implemented.")
        String message,

        @Schema(example = "2026-06-29T12:00:00Z")
        String processedAt
) {
}

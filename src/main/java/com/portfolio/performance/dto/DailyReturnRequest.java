package com.portfolio.performance.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Inbound request for a daily return calculation.
 *
 * <p>The fields are intentionally <em>not</em> annotated with bean-validation constraints. The
 * contract requires that input problems surface as an HTTP 200 response with
 * {@code status = INVALID_INPUT} and an explanatory reason, rather than a 400. That logic lives in
 * {@link com.portfolio.performance.validation.DailyReturnValidator}.
 *
 * @param portfolioId        identifier of the portfolio being valued
 * @param valuationDate      the date the valuation applies to (ISO-8601, e.g. 2026-06-29)
 * @param beginMarketValue   portfolio market value at the start of the day
 * @param endMarketValue     portfolio market value at the end of the day
 * @param netCashFlow        net external cash flow during the day (contributions positive, withdrawals negative)
 * @param benchmarkReturnPct the benchmark's return for the day, as a percentage
 * @param currency           ISO currency code the values are expressed in (e.g. USD)
 * @param requestedBy        identifier of the user or system that requested the calculation
 */
@Schema(description = "Inputs required to calculate a portfolio's daily return summary.")
public record DailyReturnRequest(

        @Schema(example = "PORT-001")
        String portfolioId,

        @Schema(example = "2026-06-29")
        LocalDate valuationDate,

        @Schema(example = "1000000.00")
        BigDecimal beginMarketValue,

        @Schema(example = "1012500.00")
        BigDecimal endMarketValue,

        @Schema(example = "0.00")
        BigDecimal netCashFlow,

        @Schema(example = "1.10")
        BigDecimal benchmarkReturnPct,

        @Schema(example = "USD")
        String currency,

        @Schema(example = "analyst.jane")
        String requestedBy
) {
}

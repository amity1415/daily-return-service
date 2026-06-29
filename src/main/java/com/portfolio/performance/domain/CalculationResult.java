package com.portfolio.performance.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Outcome of the daily return calculation: the computed figures plus the status decision and the
 * reasons behind it. This is a plain value object with no HTTP awareness.
 *
 * @param portfolioReturnPct the portfolio's daily return, as a percentage
 * @param excessReturnPct    portfolio return minus benchmark return, as a percentage
 * @param status             {@link ReturnStatus#VALID} or {@link ReturnStatus#REVIEW_REQUIRED}
 * @param reasons            explanations for a REVIEW_REQUIRED status; empty when VALID
 */
public record CalculationResult(
        BigDecimal portfolioReturnPct,
        BigDecimal excessReturnPct,
        ReturnStatus status,
        List<String> reasons
) {
}

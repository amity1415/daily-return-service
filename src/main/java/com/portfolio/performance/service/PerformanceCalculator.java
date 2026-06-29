package com.portfolio.performance.service;

import com.portfolio.performance.domain.CalculationResult;
import com.portfolio.performance.domain.ReturnStatus;
import com.portfolio.performance.dto.DailyReturnRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure business logic for the daily return calculation. Deliberately free of any HTTP or framework
 * concerns: it takes a validated request and returns a {@link CalculationResult}.
 *
 * <p>Assumes the request has already passed {@link com.portfolio.performance.validation.DailyReturnValidator}
 * (so market values are non-negative and a zero opening value implies a zero closing value). For
 * robustness it still treats absent numeric inputs leniently: a null {@code netCashFlow} or
 * {@code benchmarkReturnPct} is read as zero, and the return is only computed when
 * {@code beginMarketValue > 0} — otherwise it is zero.
 */
@Component
public class PerformanceCalculator {

    static final String REASON_BENCHMARK_DRIFT =
            "Portfolio return deviates from the benchmark by more than 5%";
    static final String REASON_CASH_FLOW_SPIKE =
            "Net cash flow exceeds 20% of the begin market value";

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal DRIFT_THRESHOLD_PCT = new BigDecimal("5");
    private static final BigDecimal CASH_FLOW_THRESHOLD_RATIO = new BigDecimal("0.20");

    /** Working precision for the division before the result is normalized. */
    private static final int CALC_SCALE = 10;

    public CalculationResult calculate(DailyReturnRequest request) {
        BigDecimal begin = request.beginMarketValue();
        BigDecimal end = nullToZero(request.endMarketValue());
        BigDecimal netCashFlow = nullToZero(request.netCashFlow());
        BigDecimal benchmark = nullToZero(request.benchmarkReturnPct());

        BigDecimal portfolioReturnPct = computePortfolioReturnPct(begin, end, netCashFlow);
        BigDecimal excessReturnPct = normalize(portfolioReturnPct.subtract(benchmark));

        List<String> reasons = new ArrayList<>();

        // Condition A — benchmark drift: |portfolioReturn - benchmark| > 5%.
        if (portfolioReturnPct.subtract(benchmark).abs().compareTo(DRIFT_THRESHOLD_PCT) > 0) {
            reasons.add(REASON_BENCHMARK_DRIFT);
        }

        // Condition B — cash flow spike: |netCashFlow| > 20% of begin market value.
        BigDecimal cashFlowThreshold = nullToZero(begin).multiply(CASH_FLOW_THRESHOLD_RATIO);
        if (netCashFlow.abs().compareTo(cashFlowThreshold) > 0) {
            reasons.add(REASON_CASH_FLOW_SPIKE);
        }

        ReturnStatus status = reasons.isEmpty() ? ReturnStatus.VALID : ReturnStatus.REVIEW_REQUIRED;
        return new CalculationResult(portfolioReturnPct, excessReturnPct, status, reasons);
    }

    private static BigDecimal computePortfolioReturnPct(BigDecimal begin, BigDecimal end, BigDecimal netCashFlow) {
        // The formula is only meaningful for a positive opening value; otherwise the return is 0
        // (covers the validated begin == 0 && end == 0 case, and is null-safe).
        if (begin == null || begin.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal gain = end.subtract(begin).subtract(netCashFlow);
        BigDecimal ratio = gain.divide(begin, CALC_SCALE, RoundingMode.HALF_UP);
        return normalize(ratio.multiply(HUNDRED));
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /** Trim trailing zeros without slipping into scientific notation (e.g. 2.500 -> 2.5, 100 -> 100). */
    private static BigDecimal normalize(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        return stripped.scale() < 0 ? stripped.setScale(0, RoundingMode.UNNECESSARY) : stripped;
    }
}

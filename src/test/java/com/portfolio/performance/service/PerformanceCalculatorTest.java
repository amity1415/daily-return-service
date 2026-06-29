package com.portfolio.performance.service;

import com.portfolio.performance.domain.CalculationResult;
import com.portfolio.performance.domain.ReturnStatus;
import com.portfolio.performance.dto.DailyReturnRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PerformanceCalculatorTest {

    private final PerformanceCalculator calculator = new PerformanceCalculator();

    private DailyReturnRequest request(String begin, String end, String netCashFlow, String benchmark) {
        return new DailyReturnRequest(
                "PORT-001",
                LocalDate.of(2026, 6, 29),
                new BigDecimal(begin),
                new BigDecimal(end),
                new BigDecimal(netCashFlow),
                benchmark == null ? null : new BigDecimal(benchmark),
                "USD",
                "analyst.jane");
    }

    @Test
    void goalExample_returns2_5_and_0_7_andValid() {
        CalculationResult result = calculator.calculate(
                request("1000000", "1035000", "10000", "1.8"));

        assertThat(result.portfolioReturnPct()).isEqualByComparingTo("2.5");
        assertThat(result.excessReturnPct()).isEqualByComparingTo("0.7");
        assertThat(result.status()).isEqualTo(ReturnStatus.VALID);
        assertThat(result.reasons()).isEmpty();
    }

    @Test
    void zeroBeginAndZeroEnd_returnsZeroReturn_andValid() {
        CalculationResult result = calculator.calculate(request("0", "0", "0", "0"));

        assertThat(result.portfolioReturnPct()).isEqualByComparingTo("0");
        assertThat(result.excessReturnPct()).isEqualByComparingTo("0");
        assertThat(result.status()).isEqualTo(ReturnStatus.VALID);
    }

    @Test
    void benchmarkDrift_overFivePercent_triggersReviewRequired() {
        // portfolioReturn = 10%, benchmark = 1% -> drift 9% (> 5%); cash flow is small.
        CalculationResult result = calculator.calculate(
                request("1000000", "1100000", "0", "1"));

        assertThat(result.portfolioReturnPct()).isEqualByComparingTo("10");
        assertThat(result.status()).isEqualTo(ReturnStatus.REVIEW_REQUIRED);
        assertThat(result.reasons()).containsExactly(PerformanceCalculator.REASON_BENCHMARK_DRIFT);
    }

    @Test
    void cashFlowSpike_overTwentyPercent_triggersReviewRequired() {
        // netCashFlow 300k > 20% of 1m (200k). Return = (1,300,000-1,000,000-300,000)/1m = 0% so no drift.
        CalculationResult result = calculator.calculate(
                request("1000000", "1300000", "300000", "0"));

        assertThat(result.portfolioReturnPct()).isEqualByComparingTo("0");
        assertThat(result.status()).isEqualTo(ReturnStatus.REVIEW_REQUIRED);
        assertThat(result.reasons()).containsExactly(PerformanceCalculator.REASON_CASH_FLOW_SPIKE);
    }

    @Test
    void bothConditions_triggerReviewRequired_withBothReasons() {
        // Big gain (drift) and a large cash flow (spike) at once.
        CalculationResult result = calculator.calculate(
                request("1000000", "1500000", "300000", "1"));

        assertThat(result.status()).isEqualTo(ReturnStatus.REVIEW_REQUIRED);
        assertThat(result.reasons()).containsExactlyInAnyOrder(
                PerformanceCalculator.REASON_BENCHMARK_DRIFT,
                PerformanceCalculator.REASON_CASH_FLOW_SPIKE);
    }

    @Test
    void driftExactlyFivePercent_doesNotTrigger() {
        // portfolioReturn = 5%, benchmark = 0% -> drift exactly 5%, which is not "greater than".
        CalculationResult result = calculator.calculate(
                request("1000000", "1050000", "0", "0"));

        assertThat(result.portfolioReturnPct()).isEqualByComparingTo("5");
        assertThat(result.status()).isEqualTo(ReturnStatus.VALID);
    }

    @Test
    void cashFlowExactlyTwentyPercent_doesNotTrigger() {
        // netCashFlow exactly 200k == 20% of 1m, which is not "greater than".
        CalculationResult result = calculator.calculate(
                request("1000000", "1200000", "200000", "0"));

        assertThat(result.status()).isEqualTo(ReturnStatus.VALID);
    }

    @Test
    void nullBenchmark_isTreatedAsZero() {
        CalculationResult result = calculator.calculate(
                request("1000000", "1035000", "10000", null));

        assertThat(result.portfolioReturnPct()).isEqualByComparingTo("2.5");
        assertThat(result.excessReturnPct()).isEqualByComparingTo("2.5");
        assertThat(result.status()).isEqualTo(ReturnStatus.VALID);
    }
}

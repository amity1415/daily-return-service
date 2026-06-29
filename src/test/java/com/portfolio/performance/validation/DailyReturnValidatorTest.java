package com.portfolio.performance.validation;

import com.portfolio.performance.dto.DailyReturnRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DailyReturnValidatorTest {

    private final DailyReturnValidator validator = new DailyReturnValidator();

    private DailyReturnRequest request(BigDecimal begin, BigDecimal end, String currency) {
        return new DailyReturnRequest(
                "PORT-001",
                LocalDate.of(2026, 6, 29),
                begin,
                end,
                BigDecimal.ZERO,
                new BigDecimal("1.10"),
                currency,
                "analyst.jane");
    }

    @Test
    void validRequest_hasNoReasons() {
        List<String> reasons = validator.validate(
                request(new BigDecimal("1000000"), new BigDecimal("1012500"), "USD"));
        assertThat(reasons).isEmpty();
    }

    @Test
    void negativeBeginMarketValue_isReported() {
        List<String> reasons = validator.validate(
                request(new BigDecimal("-1"), new BigDecimal("100"), "USD"));
        assertThat(reasons).contains(DailyReturnValidator.REASON_BEGIN_NEGATIVE);
    }

    @Test
    void negativeEndMarketValue_isReported() {
        List<String> reasons = validator.validate(
                request(new BigDecimal("100"), new BigDecimal("-1"), "USD"));
        assertThat(reasons).contains(DailyReturnValidator.REASON_END_NEGATIVE);
    }

    @Test
    void blankCurrency_isReported() {
        List<String> reasons = validator.validate(
                request(new BigDecimal("100"), new BigDecimal("100"), "  "));
        assertThat(reasons).contains(DailyReturnValidator.REASON_CURRENCY_REQUIRED);
    }

    @Test
    void nullCurrency_isReported() {
        List<String> reasons = validator.validate(
                request(new BigDecimal("100"), new BigDecimal("100"), null));
        assertThat(reasons).contains(DailyReturnValidator.REASON_CURRENCY_REQUIRED);
    }

    @Test
    void zeroBeginWithNonZeroEnd_isReported() {
        List<String> reasons = validator.validate(
                request(BigDecimal.ZERO, new BigDecimal("100"), "USD"));
        assertThat(reasons).contains(DailyReturnValidator.REASON_ZERO_BEGIN_NONZERO_END);
    }

    @Test
    void zeroBeginWithZeroEnd_isValid() {
        List<String> reasons = validator.validate(
                request(BigDecimal.ZERO, BigDecimal.ZERO, "USD"));
        assertThat(reasons).isEmpty();
    }
}

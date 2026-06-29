package com.portfolio.performance.validation;

import com.portfolio.performance.dto.DailyReturnRequest;
import com.portfolio.performance.dto.FieldError;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

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
    void validRequest_hasNoErrors() {
        List<FieldError> errors = validator.validate(
                request(new BigDecimal("1000000"), new BigDecimal("1012500"), "USD"));
        assertThat(errors).isEmpty();
    }

    @Test
    void negativeBeginMarketValue_isAttributedToBeginField() {
        List<FieldError> errors = validator.validate(
                request(new BigDecimal("-1"), new BigDecimal("100"), "USD"));
        assertThat(errors).extracting(FieldError::field, FieldError::message)
                .contains(tuple(DailyReturnValidator.FIELD_BEGIN, DailyReturnValidator.REASON_BEGIN_NEGATIVE));
    }

    @Test
    void negativeEndMarketValue_isAttributedToEndField() {
        List<FieldError> errors = validator.validate(
                request(new BigDecimal("100"), new BigDecimal("-1"), "USD"));
        assertThat(errors).extracting(FieldError::field, FieldError::message)
                .contains(tuple(DailyReturnValidator.FIELD_END, DailyReturnValidator.REASON_END_NEGATIVE));
    }

    @Test
    void blankCurrency_isAttributedToCurrencyField() {
        List<FieldError> errors = validator.validate(
                request(new BigDecimal("100"), new BigDecimal("100"), "  "));
        assertThat(errors).extracting(FieldError::field, FieldError::message)
                .contains(tuple(DailyReturnValidator.FIELD_CURRENCY, DailyReturnValidator.REASON_CURRENCY_REQUIRED));
    }

    @Test
    void nullCurrency_isAttributedToCurrencyField() {
        List<FieldError> errors = validator.validate(
                request(new BigDecimal("100"), new BigDecimal("100"), null));
        assertThat(errors).extracting(FieldError::field)
                .contains(DailyReturnValidator.FIELD_CURRENCY);
    }

    @Test
    void zeroBeginWithNonZeroEnd_isAttributedToBeginField() {
        List<FieldError> errors = validator.validate(
                request(BigDecimal.ZERO, new BigDecimal("100"), "USD"));
        assertThat(errors).extracting(FieldError::field, FieldError::message)
                .contains(tuple(DailyReturnValidator.FIELD_BEGIN, DailyReturnValidator.REASON_ZERO_BEGIN_NONZERO_END));
    }

    @Test
    void zeroBeginWithZeroEnd_isValid() {
        List<FieldError> errors = validator.validate(
                request(BigDecimal.ZERO, BigDecimal.ZERO, "USD"));
        assertThat(errors).isEmpty();
    }

    @Test
    void multipleViolations_areAllReported() {
        // Negative begin AND blank currency.
        List<FieldError> errors = validator.validate(
                request(new BigDecimal("-5"), new BigDecimal("100"), ""));
        assertThat(errors).extracting(FieldError::field)
                .contains(DailyReturnValidator.FIELD_BEGIN, DailyReturnValidator.FIELD_CURRENCY);
    }
}

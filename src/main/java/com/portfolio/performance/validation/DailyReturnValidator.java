package com.portfolio.performance.validation;

import com.portfolio.performance.dto.DailyReturnRequest;
import com.portfolio.performance.dto.FieldError;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Applies the input-validation rules for a daily return request.
 *
 * <p>Each violated rule contributes a {@link FieldError} naming the offending field. An empty result
 * means the request is acceptable for processing. By design these checks never throw — invalid input
 * is a normal, reportable outcome (HTTP 200 with {@code status = INVALID_INPUT}), not an error.
 */
@Component
public class DailyReturnValidator {

    static final String FIELD_BEGIN = "beginMarketValue";
    static final String FIELD_END = "endMarketValue";
    static final String FIELD_CURRENCY = "currency";

    static final String REASON_BEGIN_NEGATIVE = "beginMarketValue must not be negative";
    static final String REASON_END_NEGATIVE = "endMarketValue must not be negative";
    static final String REASON_CURRENCY_REQUIRED = "currency is required and must not be blank";
    static final String REASON_ZERO_BEGIN_NONZERO_END =
            "beginMarketValue is 0 while endMarketValue is non-zero, which is not a valid state";

    /**
     * @return the field-attributed errors making the request invalid; empty if it passes all checks
     */
    public List<FieldError> validate(DailyReturnRequest request) {
        List<FieldError> errors = new ArrayList<>();

        // Rule 1: market values must not be negative.
        if (isNegative(request.beginMarketValue())) {
            errors.add(new FieldError(FIELD_BEGIN, REASON_BEGIN_NEGATIVE));
        }
        if (isNegative(request.endMarketValue())) {
            errors.add(new FieldError(FIELD_END, REASON_END_NEGATIVE));
        }

        // Rule 2: currency must be present and non-blank.
        if (request.currency() == null || request.currency().isBlank()) {
            errors.add(new FieldError(FIELD_CURRENCY, REASON_CURRENCY_REQUIRED));
        }

        // Rule 3: a zero opening value with a non-zero closing value is contradictory.
        // Attributed to beginMarketValue as the primary field of the rule.
        if (isZero(request.beginMarketValue()) && isNonZero(request.endMarketValue())) {
            errors.add(new FieldError(FIELD_BEGIN, REASON_ZERO_BEGIN_NONZERO_END));
        }

        return errors;
    }

    private static boolean isNegative(BigDecimal value) {
        return value != null && value.signum() < 0;
    }

    private static boolean isZero(BigDecimal value) {
        return value != null && value.signum() == 0;
    }

    private static boolean isNonZero(BigDecimal value) {
        return value != null && value.signum() != 0;
    }
}

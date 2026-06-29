package com.portfolio.performance.validation;

import com.portfolio.performance.dto.DailyReturnRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Applies the input-validation rules for a daily return request.
 *
 * <p>Each violated rule contributes a human-readable reason. An empty result means the request is
 * structurally acceptable for processing. By design these checks never throw — invalid input is a
 * normal, reportable outcome (HTTP 200 with {@code status = INVALID_INPUT}), not an error.
 */
@Component
public class DailyReturnValidator {

    static final String REASON_BEGIN_NEGATIVE = "beginMarketValue must not be negative";
    static final String REASON_END_NEGATIVE = "endMarketValue must not be negative";
    static final String REASON_CURRENCY_REQUIRED = "currency is required and must not be blank";
    static final String REASON_ZERO_BEGIN_NONZERO_END =
            "beginMarketValue is 0 while endMarketValue is non-zero, which is not a valid state";

    /**
     * @return the list of reasons the request is invalid; empty if it passes all checks
     */
    public List<String> validate(DailyReturnRequest request) {
        List<String> reasons = new ArrayList<>();

        // Rule 1: market values must not be negative.
        if (isNegative(request.beginMarketValue())) {
            reasons.add(REASON_BEGIN_NEGATIVE);
        }
        if (isNegative(request.endMarketValue())) {
            reasons.add(REASON_END_NEGATIVE);
        }

        // Rule 2: currency must be present and non-blank.
        if (request.currency() == null || request.currency().isBlank()) {
            reasons.add(REASON_CURRENCY_REQUIRED);
        }

        // Rule 3: a zero opening value with a non-zero closing value is contradictory.
        if (isZero(request.beginMarketValue()) && isNonZero(request.endMarketValue())) {
            reasons.add(REASON_ZERO_BEGIN_NONZERO_END);
        }

        return reasons;
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

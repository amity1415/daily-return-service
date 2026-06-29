package com.portfolio.performance.service;

import com.portfolio.performance.domain.CalculationResult;
import com.portfolio.performance.domain.ReturnStatus;
import com.portfolio.performance.dto.DailyReturnRequest;
import com.portfolio.performance.dto.DailyReturnResponse;
import com.portfolio.performance.validation.DailyReturnValidator;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Orchestrates a daily return request: validate, then (if valid) calculate, and assemble the
 * response. The numeric work lives in {@link PerformanceCalculator} and the input rules in
 * {@link DailyReturnValidator}; this class only coordinates them and stamps the response.
 */
@Service
public class PerformanceService {

    private final DailyReturnValidator validator;
    private final PerformanceCalculator calculator;
    private final Clock clock;

    public PerformanceService(DailyReturnValidator validator,
                              PerformanceCalculator calculator,
                              Clock clock) {
        this.validator = validator;
        this.calculator = calculator;
        this.clock = clock;
    }

    public DailyReturnResponse process(DailyReturnRequest request) {
        String processedAt = OffsetDateTime.now(clock).toString();

        List<String> validationReasons = validator.validate(request);
        if (!validationReasons.isEmpty()) {
            return new DailyReturnResponse(
                    request.portfolioId(),
                    request.valuationDate(),
                    BigDecimal.ZERO,
                    request.benchmarkReturnPct(),
                    BigDecimal.ZERO,
                    ReturnStatus.INVALID_INPUT,
                    validationReasons,
                    processedAt);
        }

        CalculationResult result = calculator.calculate(request);
        return new DailyReturnResponse(
                request.portfolioId(),
                request.valuationDate(),
                result.portfolioReturnPct(),
                request.benchmarkReturnPct(),
                result.excessReturnPct(),
                result.status(),
                result.reasons(),
                processedAt);
    }
}

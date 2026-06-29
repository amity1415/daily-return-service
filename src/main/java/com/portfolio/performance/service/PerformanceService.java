package com.portfolio.performance.service;

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
 * Business layer for daily return processing.
 *
 * <p>Currently it validates the request and decides a status. The return percentages are still
 * placeholders ({@code 0}); the calculation step will replace the {@link ReturnStatus#VALID} branch
 * with real figures and may also produce {@link ReturnStatus#REVIEW_REQUIRED}.
 */
@Service
public class PerformanceService {

    private final DailyReturnValidator validator;
    private final Clock clock;

    public PerformanceService(DailyReturnValidator validator, Clock clock) {
        this.validator = validator;
        this.clock = clock;
    }

    public DailyReturnResponse process(DailyReturnRequest request) {
        String processedAt = OffsetDateTime.now(clock).toString();

        List<String> reasons = validator.validate(request);
        if (!reasons.isEmpty()) {
            return buildResponse(request, ReturnStatus.INVALID_INPUT, reasons, processedAt);
        }

        // Passed all checks. Calculation comes next; status is VALID for now.
        return buildResponse(request, ReturnStatus.VALID, List.of(), processedAt);
    }

    private DailyReturnResponse buildResponse(DailyReturnRequest request,
                                              ReturnStatus status,
                                              List<String> reasons,
                                              String processedAt) {
        return new DailyReturnResponse(
                request.portfolioId(),
                request.valuationDate(),
                BigDecimal.ZERO,          // portfolioReturnPct — placeholder
                request.benchmarkReturnPct(),
                BigDecimal.ZERO,          // excessReturnPct — placeholder
                status,
                reasons,
                processedAt
        );
    }
}

package com.portfolio.performance.service;

import com.portfolio.performance.domain.ReturnStatus;
import com.portfolio.performance.dto.DailyReturnRequest;
import com.portfolio.performance.dto.DailyReturnResponse;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

/**
 * Business layer for daily return processing.
 *
 * <p>This is intentionally a placeholder: it confirms the request reached the service and returns a
 * stub response. The actual return calculation and tolerance decision will be implemented here
 * (or in a dedicated calculator collaborator) in a later iteration.
 */
@Service
public class PerformanceService {

    private final Clock clock;

    public PerformanceService(Clock clock) {
        this.clock = clock;
    }

    public DailyReturnResponse process(DailyReturnRequest request) {
        return new DailyReturnResponse(
                request.portfolioId(),
                null,
                request.benchmarkReturnPct(),
                null,
                ReturnStatus.WITHIN_TOLERANCE,
                "Endpoint reachable. Calculation not yet implemented.",
                Instant.now(clock).toString()
        );
    }
}

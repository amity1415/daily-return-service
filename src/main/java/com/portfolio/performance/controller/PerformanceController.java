package com.portfolio.performance.controller;

import com.portfolio.performance.dto.DailyReturnRequest;
import com.portfolio.performance.dto.DailyReturnResponse;
import com.portfolio.performance.service.PerformanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP boundary for the performance service. Handles transport concerns only — binding and
 * validating the request, then delegating to the service and mapping the result to a response.
 */
@RestController
@RequestMapping("/api/performance")
@Tag(name = "Performance", description = "Daily portfolio return calculation")
public class PerformanceController {

    private final PerformanceService performanceService;

    public PerformanceController(PerformanceService performanceService) {
        this.performanceService = performanceService;
    }

    @Operation(
            summary = "Calculate a portfolio's daily return summary",
            description = "Accepts the day's valuation inputs and always responds 200. The outcome is "
                    + "carried in the body: VALID, or INVALID_INPUT with explanatory reasons."
    )
    @PostMapping(
            path = "/daily-return",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DailyReturnResponse> dailyReturn(@RequestBody DailyReturnRequest request) {
        return ResponseEntity.ok(performanceService.process(request));
    }
}

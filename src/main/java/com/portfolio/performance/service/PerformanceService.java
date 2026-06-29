package com.portfolio.performance.service;

import com.portfolio.performance.domain.CalculationResult;
import com.portfolio.performance.domain.ReturnStatus;
import com.portfolio.performance.dto.DailyReturnRequest;
import com.portfolio.performance.dto.DailyReturnResponse;
import com.portfolio.performance.repository.DeduplicationKey;
import com.portfolio.performance.repository.DeduplicationResult;
import com.portfolio.performance.repository.DeduplicationStore;
import com.portfolio.performance.validation.DailyReturnValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates a daily return request: deduplicate, validate, calculate, and assemble the response,
 * emitting structured lifecycle logs throughout.
 *
 * <p>Deduplication is handled here via {@link DeduplicationStore} so the controller stays unaware of
 * it. A duplicate (same portfolioId + valuationDate) returns the original response — including its
 * original {@code processedAt} — without recomputing, regardless of whether that original was VALID,
 * REVIEW_REQUIRED, or INVALID_INPUT.
 *
 * <p>Logs deliberately carry only identifiers (portfolioId, valuationDate), a correlationId, status,
 * and validation reasons — never raw market values, cash flow, or requester identity.
 */
@Service
public class PerformanceService {

    private static final Logger log = LoggerFactory.getLogger(PerformanceService.class);

    private final DailyReturnValidator validator;
    private final PerformanceCalculator calculator;
    private final DeduplicationStore deduplicationStore;
    private final Clock clock;

    public PerformanceService(DailyReturnValidator validator,
                              PerformanceCalculator calculator,
                              DeduplicationStore deduplicationStore,
                              Clock clock) {
        this.validator = validator;
        this.calculator = calculator;
        this.deduplicationStore = deduplicationStore;
        this.clock = clock;
    }

    public DailyReturnResponse process(DailyReturnRequest request) {
        String correlationId = UUID.randomUUID().toString();
        log.info("event=REQUEST_RECEIVED correlationId={} portfolioId={} valuationDate={}",
                correlationId, request.portfolioId(), request.valuationDate());

        DeduplicationKey key = new DeduplicationKey(request.portfolioId(), request.valuationDate());
        DeduplicationResult dedup =
                deduplicationStore.getOrCompute(key, () -> computeFresh(request, correlationId));

        if (!dedup.freshlyComputed()) {
            log.info("event=DUPLICATE_DETECTED correlationId={} portfolioId={} valuationDate={} "
                            + "action=returning_cached_response status={}",
                    correlationId, request.portfolioId(), request.valuationDate(),
                    dedup.response().status());
        }

        return dedup.response();
    }

    /**
     * Runs validation and calculation for a not-yet-seen request. Invoked at most once per
     * deduplication key by the store.
     */
    private DailyReturnResponse computeFresh(DailyReturnRequest request, String correlationId) {
        log.debug("event=PROCESSING_STARTED correlationId={} portfolioId={}",
                correlationId, request.portfolioId());

        String processedAt = OffsetDateTime.now(clock).toString();

        List<String> validationReasons = validator.validate(request);
        if (!validationReasons.isEmpty()) {
            log.warn("event=VALIDATION_FAILED correlationId={} portfolioId={} reasons={}",
                    correlationId, request.portfolioId(), validationReasons);
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
        log.info("event=PROCESSING_COMPLETED correlationId={} portfolioId={} status={} reasonCount={}",
                correlationId, request.portfolioId(), result.status(), result.reasons().size());

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

package com.portfolio.performance.exception;

import com.portfolio.performance.domain.ReturnStatus;
import com.portfolio.performance.dto.DailyReturnResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Translates request-binding failures into the service's normal contract: HTTP 200 with
 * {@code status = INVALID_INPUT} and an explanatory reason, rather than a raw 4xx.
 *
 * <p>This covers problems that occur <em>before</em> the request can be bound to a
 * {@code DailyReturnRequest} — malformed JSON, or a field whose value can't be parsed into its type
 * (e.g. an invalid {@code valuationDate} or a non-numeric market value). Because no request object
 * exists at this point, the echo fields are omitted from the response.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    static final String REASON_UNREADABLE =
            "Request body is malformed or contains a field that could not be parsed into the expected type";

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<DailyReturnResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        DailyReturnResponse response = new DailyReturnResponse(
                null,
                null,
                BigDecimal.ZERO,
                null,
                BigDecimal.ZERO,
                ReturnStatus.INVALID_INPUT,
                List.of(REASON_UNREADABLE),
                OffsetDateTime.now(clock).toString()
        );
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}

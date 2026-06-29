package com.portfolio.performance.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.portfolio.performance.domain.ReturnStatus;
import com.portfolio.performance.dto.DailyReturnResponse;
import com.portfolio.performance.dto.FieldError;
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
 * (e.g. an invalid {@code valuationDate} or a non-numeric market value). When Jackson can identify
 * the offending field, it is named in the {@code errors} list; for a wholly malformed body the field
 * is {@code null}.
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
        String field = extractField(ex);
        DailyReturnResponse response = new DailyReturnResponse(
                null,
                null,
                BigDecimal.ZERO,
                null,
                BigDecimal.ZERO,
                ReturnStatus.INVALID_INPUT,
                List.of(REASON_UNREADABLE),
                List.of(new FieldError(field, REASON_UNREADABLE)),
                OffsetDateTime.now(clock).toString()
        );
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * Pulls the offending field name out of Jackson's mapping exception path, when present.
     * Returns {@code null} if the failure can't be tied to a single field (e.g. broken JSON syntax).
     */
    private static String extractField(HttpMessageNotReadableException ex) {
        if (ex.getCause() instanceof JsonMappingException mappingException
                && !mappingException.getPath().isEmpty()) {
            return mappingException.getPath().get(0).getFieldName();
        }
        return null;
    }
}

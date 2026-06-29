package com.portfolio.performance.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A single, machine-readable validation error attributed to a specific request field.
 *
 * @param field   the request field that caused the error (e.g. {@code beginMarketValue}); may be
 *                {@code null} when the problem cannot be tied to one field (e.g. a fully malformed body)
 * @param message human-readable explanation of what is wrong with that field
 */
@Schema(description = "A field-level validation error.")
public record FieldError(

        @Schema(example = "beginMarketValue")
        String field,

        @Schema(example = "beginMarketValue must not be negative")
        String message
) {
}

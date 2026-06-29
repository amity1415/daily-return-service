package com.portfolio.performance.domain;

/**
 * Outcome of processing a daily return request.
 */
public enum ReturnStatus {

    /** Input passed all checks; the result is usable. */
    VALID,

    /** Result computed but outside acceptable tolerance — needs human review. */
    REVIEW_REQUIRED,

    /** Request could not be processed because the input was invalid. */
    INVALID_INPUT
}

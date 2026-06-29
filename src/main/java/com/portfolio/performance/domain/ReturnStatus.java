package com.portfolio.performance.domain;

/**
 * Outcome of evaluating a daily return against acceptable tolerance.
 */
public enum ReturnStatus {

    /** Return computed and within tolerance. */
    WITHIN_TOLERANCE,

    /** Return computed but outside tolerance — needs human review. */
    REVIEW_REQUIRED,

    /** Request could not be evaluated because the input was invalid. */
    INVALID_INPUT
}

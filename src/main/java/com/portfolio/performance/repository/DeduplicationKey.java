package com.portfolio.performance.repository;

import java.time.LocalDate;

/**
 * Identity of a daily return request for deduplication purposes: a request is a duplicate of an
 * earlier one when both the portfolio and the valuation date match. As a record it gets correct
 * {@code equals}/{@code hashCode} for use as a map key.
 */
public record DeduplicationKey(String portfolioId, LocalDate valuationDate) {
}

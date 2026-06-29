package com.portfolio.performance.repository;

import com.portfolio.performance.dto.DailyReturnResponse;

/**
 * Outcome of a deduplicated lookup.
 *
 * @param response         the response now associated with the key (newly computed or cached)
 * @param freshlyComputed  {@code true} if it was computed on this call; {@code false} if it was
 *                         served from the cache (i.e. a duplicate request)
 */
public record DeduplicationResult(DailyReturnResponse response, boolean freshlyComputed) {
}

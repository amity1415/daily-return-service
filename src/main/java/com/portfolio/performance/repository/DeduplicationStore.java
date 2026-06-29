package com.portfolio.performance.repository;

import com.portfolio.performance.dto.DailyReturnResponse;

import java.util.function.Supplier;

/**
 * Stores the response produced for a {@link DeduplicationKey} so the same request is never
 * processed twice. Storage is an implementation detail (in-memory, distributed cache, etc.).
 */
public interface DeduplicationStore {

    /**
     * Returns the cached response for {@code key}, or computes it once via {@code computation},
     * caches it, and returns it. The computation runs at most once per key; concurrent callers for
     * the same key see a single computation.
     *
     * @return the response plus whether it was freshly computed (vs served from cache)
     */
    DeduplicationResult getOrCompute(DeduplicationKey key, Supplier<DailyReturnResponse> computation);

    /** Removes all cached entries. Primarily useful for tests and operational reset. */
    void clear();
}

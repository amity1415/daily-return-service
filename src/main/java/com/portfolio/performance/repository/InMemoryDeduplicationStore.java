package com.portfolio.performance.repository;

import com.portfolio.performance.dto.DailyReturnResponse;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * In-memory {@link DeduplicationStore} backed by a {@link ConcurrentHashMap}.
 *
 * <p>{@code computeIfAbsent} gives us atomic "compute once" semantics: even if two identical
 * requests arrive concurrently, the computation runs a single time and both callers receive the
 * same response.
 *
 * <p>Note: this cache is unbounded and lives only for the JVM's lifetime. That is sufficient for the
 * current single-instance, no-database requirement; a production multi-instance deployment would
 * need a shared, evicting store (e.g. Redis) behind this same interface.
 */
@Repository
public class InMemoryDeduplicationStore implements DeduplicationStore {

    private final Map<DeduplicationKey, DailyReturnResponse> cache = new ConcurrentHashMap<>();

    @Override
    public DeduplicationResult getOrCompute(DeduplicationKey key, Supplier<DailyReturnResponse> computation) {
        AtomicBoolean freshlyComputed = new AtomicBoolean(false);
        DailyReturnResponse response = cache.computeIfAbsent(key, k -> {
            freshlyComputed.set(true);
            return computation.get();
        });
        return new DeduplicationResult(response, freshlyComputed.get());
    }

    @Override
    public void clear() {
        cache.clear();
    }
}

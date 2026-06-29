package com.portfolio.performance.repository;

import com.portfolio.performance.domain.ReturnStatus;
import com.portfolio.performance.dto.DailyReturnResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryDeduplicationStoreTest {

    private final InMemoryDeduplicationStore store = new InMemoryDeduplicationStore();

    private DailyReturnResponse sampleResponse(String id) {
        return new DailyReturnResponse(id, LocalDate.of(2026, 6, 29),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                ReturnStatus.VALID, List.of(), "2026-06-29T00:00:00Z");
    }

    @Test
    void firstCall_computes_secondCall_servesFromCache() {
        DeduplicationKey key = new DeduplicationKey("PORT-1", LocalDate.of(2026, 6, 29));
        AtomicInteger computeCount = new AtomicInteger();

        DeduplicationResult first = store.getOrCompute(key, () -> {
            computeCount.incrementAndGet();
            return sampleResponse("PORT-1");
        });
        DeduplicationResult second = store.getOrCompute(key, () -> {
            computeCount.incrementAndGet();
            return sampleResponse("PORT-1");
        });

        assertThat(first.freshlyComputed()).isTrue();
        assertThat(second.freshlyComputed()).isFalse();
        assertThat(second.response()).isSameAs(first.response());
        assertThat(computeCount.get()).isEqualTo(1);
    }

    @Test
    void differentKey_isNotADuplicate() {
        DeduplicationKey k1 = new DeduplicationKey("PORT-1", LocalDate.of(2026, 6, 29));
        DeduplicationKey k2 = new DeduplicationKey("PORT-1", LocalDate.of(2026, 6, 30));

        store.getOrCompute(k1, () -> sampleResponse("PORT-1"));
        DeduplicationResult other = store.getOrCompute(k2, () -> sampleResponse("PORT-1"));

        assertThat(other.freshlyComputed()).isTrue();
    }

    @Test
    void clear_evictsEntries_soNextCallRecomputes() {
        DeduplicationKey key = new DeduplicationKey("PORT-1", LocalDate.of(2026, 6, 29));
        store.getOrCompute(key, () -> sampleResponse("PORT-1"));

        store.clear();

        DeduplicationResult afterClear = store.getOrCompute(key, () -> sampleResponse("PORT-1"));
        assertThat(afterClear.freshlyComputed()).isTrue();
    }
}

package io.casehub.eidos.memory;

import io.casehub.eidos.api.DegradationReason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

class InMemoryAgentStateStoreTest {

    InMemoryAgentStateStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryAgentStateStore();
    }

    @Test
    void query_returns_empty_when_no_record() {
        assertThat(store.query("agent-1")).isEmpty();
    }

    @Test
    void record_and_query_returns_reason() {
        store.record("agent-1", DegradationReason.RATE_LIMITED, Instant.now().plusSeconds(60));
        assertThat(store.query("agent-1")).contains(DegradationReason.RATE_LIMITED);
    }

    @Test
    void query_returns_empty_after_ttl_expired() {
        store.record("agent-1", DegradationReason.OVERLOADED, Instant.now().minusSeconds(1));
        assertThat(store.query("agent-1")).isEmpty();
    }

    @Test
    void clear_removes_entry() {
        store.record("agent-1", DegradationReason.RATE_LIMITED, Instant.now().plusSeconds(60));
        store.clear("agent-1");
        assertThat(store.query("agent-1")).isEmpty();
    }

    @Test
    void clear_on_absent_agent_does_not_throw() {
        assertThatCode(() -> store.clear("nonexistent")).doesNotThrowAnyException();
    }

    @Test
    void record_overwrites_previous_state() {
        store.record("agent-1", DegradationReason.RATE_LIMITED, Instant.now().plusSeconds(60));
        store.record("agent-1", DegradationReason.CONTEXT_EXHAUSTED, Instant.now().plusSeconds(60));
        assertThat(store.query("agent-1")).contains(DegradationReason.CONTEXT_EXHAUSTED);
    }

    @Test
    void different_agents_are_independent() {
        store.record("agent-1", DegradationReason.RATE_LIMITED, Instant.now().plusSeconds(60));
        assertThat(store.query("agent-2")).isEmpty();
    }

    @Test
    void concurrent_writes_do_not_corrupt_state() throws InterruptedException {
        final int threads = 8;
        final int recordsPerThread = 100;
        final var latch = new CountDownLatch(threads);
        final var errors = new AtomicInteger(0);
        final var executor = Executors.newFixedThreadPool(threads);

        for (int t = 0; t < threads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < recordsPerThread; i++) {
                        store.record("agent-" + threadId, DegradationReason.RATE_LIMITED,
                                Instant.now().plusSeconds(60));
                        store.query("agent-" + threadId);
                    }
                } catch (final Exception e) {
                    errors.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        executor.shutdownNow();
        assertThat(errors.get()).isZero();
        // Each thread's agent retains its last recorded state
        for (int t = 0; t < threads; t++) {
            assertThat(store.query("agent-" + t)).contains(DegradationReason.RATE_LIMITED);
        }
    }
}

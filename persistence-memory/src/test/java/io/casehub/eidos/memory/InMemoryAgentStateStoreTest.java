package io.casehub.eidos.memory;

import io.casehub.eidos.api.AgentStateStore;
import io.casehub.eidos.api.AgentStateStoreContractTest;
import io.casehub.eidos.api.DegradationReason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;

class InMemoryAgentStateStoreTest extends AgentStateStoreContractTest {

    private InMemoryAgentStateStore store;

    @BeforeEach
    @Override
    protected void resetStore() {
        store = new InMemoryAgentStateStore();
    }

    @Override
    protected AgentStateStore store() {
        return store;
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
                        store.record("agent-" + threadId, "default", DegradationReason.RATE_LIMITED,
                                Instant.now().plusSeconds(60));
                        store.query("agent-" + threadId, "default");
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
        for (int t = 0; t < threads; t++) {
            assertThat(store.query("agent-" + t, "default")).contains(DegradationReason.RATE_LIMITED);
        }
    }
}

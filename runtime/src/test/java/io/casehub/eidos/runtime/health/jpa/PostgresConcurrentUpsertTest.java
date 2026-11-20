package io.casehub.eidos.runtime.health.jpa;

import io.casehub.eidos.api.AgentStateStore;
import io.casehub.eidos.api.BehavioralSignalStore;
import io.casehub.eidos.api.DegradationReason;
import io.casehub.eidos.api.DispositionSignalStore;
import io.quarkus.arc.Arc;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static io.casehub.eidos.api.BehavioralSignal.DECLINE;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@TestProfile(PostgresTestProfile.class)
@Tag("testcontainers")
class PostgresConcurrentUpsertTest {

    @Inject BehavioralSignalStore behavioralStore;
    @Inject DispositionSignalStore dispositionStore;
    @Inject AgentStateStore stateStore;
    @Inject EntityManager em;

    static final int THREADS = 10;
    static final int RECORDS_PER_THREAD = 10;
    static final int MAX_RETRIES = 5;

    @BeforeEach
    @Transactional
    void clean() {
        em.createNativeQuery("DELETE FROM behavioral_signal").executeUpdate();
        em.createNativeQuery("DELETE FROM disposition_signal").executeUpdate();
        em.createNativeQuery("DELETE FROM agent_state").executeUpdate();
    }

    @Test
    void concurrent_behavioral_signal_upsert_preserves_count() {
        var failures = new AtomicInteger();

        try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int t = 0; t < THREADS; t++) {
                exec.submit(() -> {
                    try {
                        for (int i = 0; i < RECORDS_PER_THREAD; i++) {
                            retryOnContention(() ->
                                behavioralStore.record("a1", "t1", "code-review", "rust", DECLINE));
                        }
                    } catch (Exception e) {
                        failures.incrementAndGet();
                    }
                });
            }
        }

        assertThat(failures.get()).as("concurrent failures").isZero();
        assertThat(behavioralStore.count("a1", "t1", "code-review", "rust", DECLINE))
            .isEqualTo(THREADS * RECORDS_PER_THREAD);
    }

    @Test
    void concurrent_disposition_signal_upsert_preserves_weight() {
        var failures = new AtomicInteger();

        try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int t = 0; t < THREADS; t++) {
                exec.submit(() -> {
                    try {
                        for (int i = 0; i < RECORDS_PER_THREAD; i++) {
                            retryOnContention(() ->
                                dispositionStore.recordActivation("a1", "t1", "Ti"));
                        }
                    } catch (Exception e) {
                        failures.incrementAndGet();
                    }
                });
            }
        }

        assertThat(failures.get()).as("concurrent failures").isZero();
        assertThat(dispositionStore.activationWeights("a1", "t1"))
            .containsEntry("Ti", (double) (THREADS * RECORDS_PER_THREAD));
    }

    @Test
    void concurrent_agent_state_upsert_survives_contention() {
        var failures = new AtomicInteger();
        var expiresAt = Instant.now().plusSeconds(3600);

        try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int t = 0; t < THREADS; t++) {
                exec.submit(() -> {
                    try {
                        for (int i = 0; i < RECORDS_PER_THREAD; i++) {
                            retryOnContention(() ->
                                stateStore.record("a1", "t1", DegradationReason.RATE_LIMITED, expiresAt));
                        }
                    } catch (Exception e) {
                        failures.incrementAndGet();
                    }
                });
            }
        }

        assertThat(failures.get()).as("concurrent failures").isZero();
        assertThat(stateStore.query("a1", "t1"))
            .isPresent()
            .hasValue(DegradationReason.RATE_LIMITED);
    }

    private void retryOnContention(Runnable action) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            var ctx = Arc.container().requestContext();
            ctx.activate();
            try {
                action.run();
                return;
            } catch (Exception e) {
                if (attempt == MAX_RETRIES || !isTransientPostgresError(e)) {
                    throw e instanceof RuntimeException re ? re : new RuntimeException(e);
                }
            } finally {
                ctx.deactivate();
            }
        }
    }

    private static boolean isTransientPostgresError(Throwable t) {
        while (t != null) {
            if (t instanceof SQLException sqle) {
                var state = sqle.getSQLState();
                if ("23505".equals(state) || "40P01".equals(state) || "40001".equals(state)) {
                    return true;
                }
            }
            t = t.getCause();
        }
        return false;
    }
}

package io.casehub.eidos.runtime.health.jpa;

import io.casehub.eidos.api.DegradationReason;
import io.casehub.eidos.api.ReactiveAgentStateStore;
import io.casehub.eidos.runtime.registry.ReactiveTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;

@QuarkusTest
@TestProfile(ReactiveTestProfile.class)
class JpaReactiveAgentStateStoreTest {

    @Inject
    ReactiveAgentStateStore store;

    @Test
    @RunOnVertxContext
    void record_and_query_returns_reason(UniAsserter asserter) {
        asserter
            .execute(() -> store.record("r-jpa-1", "default", DegradationReason.RATE_LIMITED, Instant.now().plusSeconds(60)))
            .assertThat(() -> store.query("r-jpa-1", "default"), result ->
                assertThat(result).contains(DegradationReason.RATE_LIMITED));
    }

    @Test
    @RunOnVertxContext
    void query_returns_empty_after_ttl_expired(UniAsserter asserter) {
        asserter
            .execute(() -> store.record("r-jpa-2", "default", DegradationReason.OVERLOADED, Instant.now().minusSeconds(1)))
            .assertThat(() -> store.query("r-jpa-2", "default"), result ->
                assertThat(result).isEmpty());
    }

    @Test
    @RunOnVertxContext
    void clear_removes_entry(UniAsserter asserter) {
        asserter
            .execute(() -> store.record("r-jpa-3", "default", DegradationReason.RATE_LIMITED, Instant.now().plusSeconds(60)))
            .execute(() -> store.clear("r-jpa-3", "default"))
            .assertThat(() -> store.query("r-jpa-3", "default"), result ->
                assertThat(result).isEmpty());
    }

    @Test
    @RunOnVertxContext
    void record_overwrites_previous_state(UniAsserter asserter) {
        asserter
            .execute(() -> store.record("r-jpa-4", "default", DegradationReason.RATE_LIMITED, Instant.now().plusSeconds(60)))
            .execute(() -> store.record("r-jpa-4", "default", DegradationReason.CONTEXT_EXHAUSTED, Instant.now().plusSeconds(60)))
            .assertThat(() -> store.query("r-jpa-4", "default"), result ->
                assertThat(result).contains(DegradationReason.CONTEXT_EXHAUSTED));
    }
}

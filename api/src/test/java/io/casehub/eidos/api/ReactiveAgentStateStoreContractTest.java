package io.casehub.eidos.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;

public abstract class ReactiveAgentStateStoreContractTest {

    protected abstract ReactiveAgentStateStore store();

    @BeforeEach
    protected void resetStore() {}

    @Test
    void query_returns_empty_when_no_record() {
        var result = store().query("r-contract-agent-1", "tenant-1").await().atMost(Duration.ofSeconds(5));
        assertThat(result).isEmpty();
    }

    @Test
    void record_and_query_returns_reason() {
        store().record("r-contract-agent-1", "tenant-1", DegradationReason.RATE_LIMITED, Instant.now().plusSeconds(60))
               .await().atMost(Duration.ofSeconds(5));
        var result = store().query("r-contract-agent-1", "tenant-1").await().atMost(Duration.ofSeconds(5));
        assertThat(result).contains(DegradationReason.RATE_LIMITED);
    }

    @Test
    void query_returns_empty_after_ttl_expired() {
        store().record("r-contract-agent-1", "tenant-1", DegradationReason.OVERLOADED, Instant.now().minusSeconds(1))
               .await().atMost(Duration.ofSeconds(5));
        var result = store().query("r-contract-agent-1", "tenant-1").await().atMost(Duration.ofSeconds(5));
        assertThat(result).isEmpty();
    }

    @Test
    void clear_removes_entry() {
        store().record("r-contract-agent-1", "tenant-1", DegradationReason.RATE_LIMITED, Instant.now().plusSeconds(60))
               .await().atMost(Duration.ofSeconds(5));
        store().clear("r-contract-agent-1", "tenant-1").await().atMost(Duration.ofSeconds(5));
        var result = store().query("r-contract-agent-1", "tenant-1").await().atMost(Duration.ofSeconds(5));
        assertThat(result).isEmpty();
    }

    @Test
    void clear_on_absent_agent_does_not_throw() {
        assertThatCode(() ->
            store().clear("r-contract-nonexistent", "tenant-1").await().atMost(Duration.ofSeconds(5))
        ).doesNotThrowAnyException();
    }

    @Test
    void record_overwrites_previous_state() {
        store().record("r-contract-agent-1", "tenant-1", DegradationReason.RATE_LIMITED, Instant.now().plusSeconds(60))
               .await().atMost(Duration.ofSeconds(5));
        store().record("r-contract-agent-1", "tenant-1", DegradationReason.CONTEXT_EXHAUSTED, Instant.now().plusSeconds(60))
               .await().atMost(Duration.ofSeconds(5));
        var result = store().query("r-contract-agent-1", "tenant-1").await().atMost(Duration.ofSeconds(5));
        assertThat(result).contains(DegradationReason.CONTEXT_EXHAUSTED);
    }

    @Test
    void different_agents_are_independent() {
        store().record("r-contract-agent-1", "tenant-1", DegradationReason.RATE_LIMITED, Instant.now().plusSeconds(60))
               .await().atMost(Duration.ofSeconds(5));
        var result = store().query("r-contract-agent-2", "tenant-1").await().atMost(Duration.ofSeconds(5));
        assertThat(result).isEmpty();
    }

    @Test
    void different_tenancies_are_isolated() {
        store().record("r-contract-agent-1", "tenant-1", DegradationReason.RATE_LIMITED, Instant.now().plusSeconds(60))
               .await().atMost(Duration.ofSeconds(5));
        store().record("r-contract-agent-1", "tenant-2", DegradationReason.CONTEXT_EXHAUSTED, Instant.now().plusSeconds(60))
               .await().atMost(Duration.ofSeconds(5));
        var result1 = store().query("r-contract-agent-1", "tenant-1").await().atMost(Duration.ofSeconds(5));
        var result2 = store().query("r-contract-agent-1", "tenant-2").await().atMost(Duration.ofSeconds(5));
        assertThat(result1).contains(DegradationReason.RATE_LIMITED);
        assertThat(result2).contains(DegradationReason.CONTEXT_EXHAUSTED);
    }
}

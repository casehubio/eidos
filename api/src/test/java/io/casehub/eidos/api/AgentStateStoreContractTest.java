package io.casehub.eidos.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;

public abstract class AgentStateStoreContractTest {

    protected abstract AgentStateStore store();

    @BeforeEach
    protected void resetStore() {}

    @Test
    void query_returns_empty_when_no_record() {
        assertThat(store().query("agent-1", "tenant-1")).isEmpty();
    }

    @Test
    void record_and_query_returns_reason() {
        store().record("agent-1", "tenant-1", DegradationReason.RATE_LIMITED, Instant.now().plusSeconds(60));
        assertThat(store().query("agent-1", "tenant-1")).contains(DegradationReason.RATE_LIMITED);
    }

    @Test
    void query_returns_empty_after_ttl_expired() {
        store().record("agent-1", "tenant-1", DegradationReason.OVERLOADED, Instant.now().minusSeconds(1));
        assertThat(store().query("agent-1", "tenant-1")).isEmpty();
    }

    @Test
    void clear_removes_entry() {
        store().record("agent-1", "tenant-1", DegradationReason.RATE_LIMITED, Instant.now().plusSeconds(60));
        store().clear("agent-1", "tenant-1");
        assertThat(store().query("agent-1", "tenant-1")).isEmpty();
    }

    @Test
    void clear_on_absent_agent_does_not_throw() {
        assertThatCode(() -> store().clear("nonexistent", "tenant-1")).doesNotThrowAnyException();
    }

    @Test
    void record_overwrites_previous_state() {
        store().record("agent-1", "tenant-1", DegradationReason.RATE_LIMITED, Instant.now().plusSeconds(60));
        store().record("agent-1", "tenant-1", DegradationReason.CONTEXT_EXHAUSTED, Instant.now().plusSeconds(60));
        assertThat(store().query("agent-1", "tenant-1")).contains(DegradationReason.CONTEXT_EXHAUSTED);
    }

    @Test
    void different_agents_are_independent() {
        store().record("agent-1", "tenant-1", DegradationReason.RATE_LIMITED, Instant.now().plusSeconds(60));
        assertThat(store().query("agent-2", "tenant-1")).isEmpty();
    }

    @Test
    void different_tenancies_are_isolated() {
        store().record("agent-1", "tenant-1", DegradationReason.RATE_LIMITED,      Instant.now().plusSeconds(60));
        store().record("agent-1", "tenant-2", DegradationReason.CONTEXT_EXHAUSTED, Instant.now().plusSeconds(60));
        assertThat(store().query("agent-1", "tenant-1")).contains(DegradationReason.RATE_LIMITED);
        assertThat(store().query("agent-1", "tenant-2")).contains(DegradationReason.CONTEXT_EXHAUSTED);
    }
}

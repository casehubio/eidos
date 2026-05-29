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
        assertThat(store().query("contract-agent-1")).isEmpty();
    }

    @Test
    void record_and_query_returns_reason() {
        store().record("contract-agent-1", DegradationReason.RATE_LIMITED, Instant.now().plusSeconds(60));
        assertThat(store().query("contract-agent-1")).contains(DegradationReason.RATE_LIMITED);
    }

    @Test
    void query_returns_empty_after_ttl_expired() {
        store().record("contract-agent-1", DegradationReason.OVERLOADED, Instant.now().minusSeconds(1));
        assertThat(store().query("contract-agent-1")).isEmpty();
    }

    @Test
    void clear_removes_entry() {
        store().record("contract-agent-1", DegradationReason.RATE_LIMITED, Instant.now().plusSeconds(60));
        store().clear("contract-agent-1");
        assertThat(store().query("contract-agent-1")).isEmpty();
    }

    @Test
    void clear_on_absent_agent_does_not_throw() {
        assertThatCode(() -> store().clear("contract-nonexistent")).doesNotThrowAnyException();
    }

    @Test
    void record_overwrites_previous_state() {
        store().record("contract-agent-1", DegradationReason.RATE_LIMITED, Instant.now().plusSeconds(60));
        store().record("contract-agent-1", DegradationReason.CONTEXT_EXHAUSTED, Instant.now().plusSeconds(60));
        assertThat(store().query("contract-agent-1")).contains(DegradationReason.CONTEXT_EXHAUSTED);
    }

    @Test
    void different_agents_are_independent() {
        store().record("contract-agent-1", DegradationReason.RATE_LIMITED, Instant.now().plusSeconds(60));
        assertThat(store().query("contract-agent-2")).isEmpty();
    }
}

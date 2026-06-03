package io.casehub.eidos.runtime.health;

import io.casehub.eidos.api.*;
import io.casehub.eidos.api.CapabilityHealth.CapabilityStatus;
import io.casehub.eidos.api.CapabilityHealth.ProbeContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.*;

class DefaultCapabilityHealthDegradedTest {

    static class StubStateStore implements AgentStateStore {
        private final ConcurrentHashMap<String, DegradationReason> state = new ConcurrentHashMap<>();

        @Override
        public void record(final String agentId, final String tenancyId,
                           final DegradationReason reason, final Instant expiresAt) {
            // TTL enforcement is tested at the store level; intentionally ignored here.
            state.put(agentId + "|" + tenancyId, reason);
        }

        @Override
        public Optional<DegradationReason> query(final String agentId, final String tenancyId) {
            return Optional.ofNullable(state.get(agentId + "|" + tenancyId));
        }

        @Override
        public void clear(final String agentId, final String tenancyId) {
            state.remove(agentId + "|" + tenancyId);
        }
    }

    StubStateStore stateStore;
    DefaultCapabilityHealth health;

    @BeforeEach
    void setUp() {
        stateStore = new StubStateStore();
        health = new DefaultCapabilityHealth(0.3, stateStore);
    }

    static AgentDescriptor agent(final String agentId, final AgentCapability... capabilities) {
        return new AgentDescriptor(
            agentId, "Agent", "1.0", "anthropic", "claude", "claude-3-7",
            null, null, null, null, "reviewer",
            List.of(capabilities),
            new AgentDisposition("collaborative", "principled", "measured", "semi-autonomous", false),
            null, null, "default"
        );
    }

    static AgentCapability capability(final String name, final Map<String, Double> epistemicDomains) {
        return new AgentCapability(name, 0.9, null, null,
            List.of(), List.of(), List.of(), epistemicDomains);
    }

    @Test
    void degraded_state_takes_precedence_over_ready() {
        stateStore.record("agent-1", "default", DegradationReason.RATE_LIMITED, Instant.now().plusSeconds(60));
        final var descriptor = agent("agent-1", capability("code-review", Map.of("java", 0.95)));
        final var status = health.probe(descriptor, "code-review", ProbeContext.of("java"));
        assertThat(status).isInstanceOf(CapabilityStatus.Degraded.class);
        assertThat(((CapabilityStatus.Degraded) status).reason()).isEqualTo(DegradationReason.RATE_LIMITED);
    }

    @Test
    void degraded_state_takes_precedence_over_epistemically_weak() {
        stateStore.record("agent-1", "default", DegradationReason.OVERLOADED, Instant.now().plusSeconds(60));
        final var descriptor = agent("agent-1", capability("code-review", Map.of("rust", 0.1)));
        final var status = health.probe(descriptor, "code-review", ProbeContext.of("rust"));
        assertThat(status).isInstanceOf(CapabilityStatus.Degraded.class);
        assertThat(((CapabilityStatus.Degraded) status).reason()).isEqualTo(DegradationReason.OVERLOADED);
    }

    @Test
    void no_degraded_state_returns_ready() {
        final var descriptor = agent("agent-1", capability("code-review", Map.of()));
        final var status = health.probe(descriptor, "code-review", ProbeContext.of(null));
        assertThat(status).isInstanceOf(CapabilityStatus.Ready.class);
    }

    @Test
    void cleared_degraded_state_allows_ready() {
        stateStore.record("agent-1", "default", DegradationReason.RATE_LIMITED, Instant.now().plusSeconds(60));
        stateStore.clear("agent-1", "default");
        final var descriptor = agent("agent-1", capability("code-review", Map.of()));
        final var status = health.probe(descriptor, "code-review", ProbeContext.of(null));
        assertThat(status).isInstanceOf(CapabilityStatus.Ready.class);
    }

    @Test
    void degraded_detail_identifies_reason() {
        stateStore.record("agent-2", "default", DegradationReason.CONTEXT_EXHAUSTED, Instant.now().plusSeconds(60));
        final var descriptor = agent("agent-2", capability("planning", Map.of()));
        final var status = health.probe(descriptor, "planning", ProbeContext.of(null));
        final var degraded = (CapabilityStatus.Degraded) status;
        assertThat(degraded.reason()).isEqualTo(DegradationReason.CONTEXT_EXHAUSTED);
        assertThat(degraded.detail()).isNotBlank();
    }
}

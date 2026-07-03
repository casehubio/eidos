package io.casehub.eidos.runtime.health;

import io.casehub.eidos.api.*;
import io.casehub.eidos.api.CapabilityHealth.CapabilityStatus;
import io.casehub.eidos.api.CapabilityHealth.ProbeContext;
import io.casehub.platform.api.preferences.PreferenceProvider;
import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
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

    static class NoOpBehavioralSignalStore implements BehavioralSignalStore {
        @Override public void record(String a, String t, String c, String d, BehavioralSignal s) {}
        @Override public void clear(String a, String t, String c, BehavioralSignal s) {}
        @Override public Map<String, Integer> learned(String a, String t, String c, BehavioralSignal s) { return Map.of(); }
        @Override public int count(String a, String t, String c, String d, BehavioralSignal s) { return 0; }
    }

    StubStateStore stateStore;
    @SuppressWarnings("unchecked")
    Instance<PreferenceProvider> preferenceProviderInstance;
    VocabularyRegistry mockVocabRegistry;
    DefaultCapabilityHealth health;

    @BeforeEach
    void setUp() {
        stateStore = new StubStateStore();
        preferenceProviderInstance = org.mockito.Mockito.mock(Instance.class);
        org.mockito.Mockito.lenient().when(preferenceProviderInstance.isUnsatisfied()).thenReturn(true);
        mockVocabRegistry = org.mockito.Mockito.mock(VocabularyRegistry.class);
        health = new DefaultCapabilityHealth(0.3, stateStore, new NoOpBehavioralSignalStore(), preferenceProviderInstance, mockVocabRegistry);
    }

    static AgentDescriptor agent(final String agentId, final AgentCapability... capabilities) {
        return AgentDescriptor.builder()
            .agentId(agentId)
            .name("Agent")
            .version("1.0")
            .provider("anthropic")
            .modelFamily("claude")
            .modelVersion("claude-3-7")
            .slot("reviewer")
            .capabilities(List.of(capabilities))
            .disposition(AgentDisposition.builder()
                .socialOrient("collaborative")
                .ruleFollowing("principled")
                .riskAppetite("measured")
                .autonomy("semi-autonomous")
                .build())
            .tenancyId("default")
            .build();
    }

    static AgentCapability capability(final String name, final Map<String, Double> epistemicDomains) {
        return AgentCapability.builder().name(name).qualityHint(0.9)
            .epistemicDomains(epistemicDomains).build();
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

    @Test
    void degraded_state_takes_precedence_over_declared_exclusion() {
        stateStore.record("agent-x", "default", DegradationReason.RATE_LIMITED, Instant.now().plusSeconds(60));
        final var descriptor = agent("agent-x",
            AgentCapability.builder().name("code-review").qualityHint(0.9)
                .excludedDomains(Set.of("rust")).build());
        final var status = health.probe(descriptor, "code-review", ProbeContext.of("rust"));
        assertThat(status).isInstanceOf(CapabilityStatus.Degraded.class);
    }
}

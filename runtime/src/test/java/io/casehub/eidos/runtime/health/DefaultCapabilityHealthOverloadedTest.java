package io.casehub.eidos.runtime.health;

import io.casehub.eidos.api.*;
import io.casehub.eidos.api.CapabilityHealth.CapabilityStatus;
import io.casehub.eidos.api.CapabilityHealth.ProbeContext;
import io.casehub.platform.api.capacity.ActorCapacityView;
import io.casehub.platform.api.capacity.CapacitySignal;
import io.casehub.platform.api.preferences.PreferenceProvider;
import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DefaultCapabilityHealthOverloadedTest {

    static class NoOpStateStore implements AgentStateStore {
        @Override public void record(String a, String t, DegradationReason r, Instant e) {}
        @Override public Optional<DegradationReason> query(String a, String t) { return Optional.empty(); }
        @Override public void clear(String a, String t) {}
    }

    static class NoOpSignalStore implements BehavioralSignalStore {
        @Override public void record(String a, String t, String c, String d, BehavioralSignal s) {}
        @Override public void clear(String a, String t, String c, BehavioralSignal s) {}
        @Override public Map<String, Integer> learned(String a, String t, String c, BehavioralSignal s) { return Map.of(); }
        @Override public int count(String a, String t, String c, String d, BehavioralSignal s) { return 0; }
    }

    ActorCapacityView capacityView;
    @SuppressWarnings("unchecked")
    Instance<ActorCapacityView> capacityViewInstance = mock(Instance.class);
    @SuppressWarnings("unchecked")
    Instance<PreferenceProvider> prefProvider = mock(Instance.class);
    DefaultCapabilityHealth health;

    @BeforeEach
    void setUp() {
        capacityView = mock(ActorCapacityView.class);
        capacityViewInstance = mock(Instance.class);
        when(capacityViewInstance.isResolvable()).thenReturn(true);
        when(capacityViewInstance.get()).thenReturn(capacityView);
        lenient().when(prefProvider.isUnsatisfied()).thenReturn(true);
        health = new DefaultCapabilityHealth(0.3, 0.8, new NoOpStateStore(),
                new NoOpSignalStore(), prefProvider, capacityViewInstance,
                mock(VocabularyRegistry.class));
    }

    static AgentDescriptor agent(String agentId, AgentCapability... capabilities) {
        return AgentDescriptor.builder()
            .agentId(agentId).name("Agent").version("1.0")
            .provider("anthropic").modelFamily("claude").modelVersion("claude-3-7")
            .slot("worker")
            .capabilities(List.of(capabilities))
            .disposition(AgentDisposition.builder()
                .socialOrient("collaborative").ruleFollowing("principled")
                .riskAppetite("measured").autonomy("semi-autonomous").build())
            .tenancyId("default").build();
    }

    @Test
    void overloaded_above_threshold_returns_overloaded() {
        when(capacityView.aggregatedPressure("agent-1"))
            .thenReturn(new CapacitySignal("agent-1", "test", 0.95, Instant.now()));
        var descriptor = agent("agent-1",
            AgentCapability.builder().name("code-review").build());
        var status = health.probe(descriptor, "code-review", ProbeContext.of(null));
        assertThat(status).isInstanceOf(CapabilityStatus.Overloaded.class);
        var overloaded = (CapabilityStatus.Overloaded) status;
        assertThat(overloaded.pressure()).isEqualTo(0.95);
        assertThat(overloaded.threshold()).isEqualTo(0.8);
    }

    @Test
    void overloaded_at_threshold_returns_overloaded() {
        when(capacityView.aggregatedPressure("agent-1"))
            .thenReturn(new CapacitySignal("agent-1", "test", 0.8, Instant.now()));
        var descriptor = agent("agent-1",
            AgentCapability.builder().name("code-review").build());
        var status = health.probe(descriptor, "code-review", ProbeContext.of(null));
        assertThat(status).isInstanceOf(CapabilityStatus.Overloaded.class);
    }

    @Test
    void below_threshold_returns_ready() {
        when(capacityView.aggregatedPressure("agent-1"))
            .thenReturn(new CapacitySignal("agent-1", "test", 0.5, Instant.now()));
        var descriptor = agent("agent-1",
            AgentCapability.builder().name("code-review").build());
        var status = health.probe(descriptor, "code-review", ProbeContext.of(null));
        assertThat(status).isInstanceOf(CapabilityStatus.Ready.class);
    }

    @Test
    void null_signal_falls_through_to_ready() {
        when(capacityView.aggregatedPressure("agent-1")).thenReturn(null);
        var descriptor = agent("agent-1",
            AgentCapability.builder().name("code-review").build());
        var status = health.probe(descriptor, "code-review", ProbeContext.of(null));
        assertThat(status).isInstanceOf(CapabilityStatus.Ready.class);
    }

    @Test
    void no_capacity_view_deployed_falls_through_to_ready() {
        @SuppressWarnings("unchecked")
        Instance<ActorCapacityView> noCapacity = mock(Instance.class);
        when(noCapacity.isResolvable()).thenReturn(false);
        var healthNoCapacity = new DefaultCapabilityHealth(0.3, 0.8, new NoOpStateStore(),
                new NoOpSignalStore(), prefProvider, noCapacity,
                mock(VocabularyRegistry.class));
        var descriptor = agent("agent-1",
            AgentCapability.builder().name("code-review").build());
        var status = healthNoCapacity.probe(descriptor, "code-review", ProbeContext.of(null));
        assertThat(status).isInstanceOf(CapabilityStatus.Ready.class);
    }

    @Test
    void custom_threshold_respected() {
        var customHealth = new DefaultCapabilityHealth(0.3, 0.6, new NoOpStateStore(),
                new NoOpSignalStore(), prefProvider, capacityViewInstance,
                mock(VocabularyRegistry.class));
        when(capacityView.aggregatedPressure("agent-1"))
            .thenReturn(new CapacitySignal("agent-1", "test", 0.7, Instant.now()));
        var descriptor = agent("agent-1",
            AgentCapability.builder().name("code-review").build());
        var status = customHealth.probe(descriptor, "code-review", ProbeContext.of(null));
        assertThat(status).isInstanceOf(CapabilityStatus.Overloaded.class);
        assertThat(((CapabilityStatus.Overloaded) status).threshold()).isEqualTo(0.6);
    }

    @Test
    void degraded_takes_precedence_over_overloaded() {
        var stateStore = new DefaultCapabilityHealthDegradedTest.StubStateStore();
        stateStore.record("agent-1", "default", DegradationReason.RATE_LIMITED,
            Instant.now().plusSeconds(60));
        when(capacityView.aggregatedPressure("agent-1"))
            .thenReturn(new CapacitySignal("agent-1", "test", 0.95, Instant.now()));
        var healthWithState = new DefaultCapabilityHealth(0.3, 0.8, stateStore,
                new NoOpSignalStore(), prefProvider, capacityViewInstance,
                mock(VocabularyRegistry.class));
        var descriptor = agent("agent-1",
            AgentCapability.builder().name("code-review").build());
        var status = healthWithState.probe(descriptor, "code-review", ProbeContext.of(null));
        assertThat(status).isInstanceOf(CapabilityStatus.Degraded.class);
    }

    @Test
    void overloaded_takes_precedence_over_unavailable() {
        when(capacityView.aggregatedPressure("agent-1"))
            .thenReturn(new CapacitySignal("agent-1", "test", 0.95, Instant.now()));
        var descriptor = agent("agent-1");
        var status = health.probe(descriptor, "missing-capability", ProbeContext.of(null));
        assertThat(status).isInstanceOf(CapabilityStatus.Overloaded.class);
    }
}

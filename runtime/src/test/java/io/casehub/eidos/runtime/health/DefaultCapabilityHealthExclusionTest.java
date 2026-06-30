package io.casehub.eidos.runtime.health;

import io.casehub.eidos.api.*;
import io.casehub.eidos.api.CapabilityHealth.CapabilityStatus;
import io.casehub.eidos.api.CapabilityHealth.ProbeContext;
import io.casehub.platform.api.preferences.PreferenceProvider;
import io.casehub.platform.api.preferences.Preferences;
import io.casehub.platform.api.preferences.SettingsScope;
import io.casehub.eidos.runtime.preferences.EidosPreferenceKeys;
import io.casehub.eidos.runtime.preferences.ExcludeThresholdPreference;
import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.casehub.eidos.api.SpecializationSignal;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class DefaultCapabilityHealthExclusionTest {

    static class StubStateStore implements AgentStateStore {
        @Override public void record(String a, String t, DegradationReason r, Instant e) {}
        @Override public Optional<DegradationReason> query(String a, String t) { return Optional.empty(); }
        @Override public void clear(String a, String t) {}
    }

    static class StubSpecializationStore implements CapabilitySpecializationStore {
        private final Map<String, Integer> counts = new HashMap<>();

        void setCount(String agentId, String tenancyId, String capability,
                       String domain, SpecializationSignal signal, int count) {
            counts.put(agentId + "|" + tenancyId + "|" + capability + "|" + domain + "|" + signal.name(), count);
        }

        @Override public void record(String a, String t, String c, String d, SpecializationSignal s) {}
        @Override public void clear(String a, String t, String c, SpecializationSignal s) {}
        @Override public Map<String, Integer> learned(String a, String t, String c, SpecializationSignal s) { return Map.of(); }

        @Override
        public int count(String agentId, String tenancyId, String capabilityName,
                          String domain, SpecializationSignal signal) {
            return counts.getOrDefault(
                agentId + "|" + tenancyId + "|" + capabilityName + "|" + domain + "|" + signal.name(), 0);
        }
    }

    @Mock
    @SuppressWarnings("unchecked")
    Instance<PreferenceProvider> preferenceProviderInstance;

    StubStateStore stateStore;
    StubSpecializationStore specializationStore;
    DefaultCapabilityHealth health;

    @BeforeEach
    void setUp() {
        stateStore = new StubStateStore();
        specializationStore = new StubSpecializationStore();
        lenient().when(preferenceProviderInstance.isUnsatisfied()).thenReturn(true);
        health = new DefaultCapabilityHealth(0.3, stateStore, specializationStore, preferenceProviderInstance);
    }

    static AgentDescriptor agent(String agentId, AgentCapability... capabilities) {
        return AgentDescriptor.builder()
            .agentId(agentId).name("Agent").version("1.0").provider("anthropic")
            .modelFamily("claude").modelVersion("claude-3-7").slot("reviewer")
            .capabilities(List.of(capabilities))
            .disposition(AgentDisposition.builder()
                .socialOrient("collaborative")
                .ruleFollowing("principled")
                .riskAppetite("measured")
                .autonomy("semi-autonomous")
                .build())
            .tenancyId("default").build();
    }

    static AgentCapability capabilityWithExclusions(String name, Set<String> excludedDomains) {
        return AgentCapability.builder().name(name).qualityHint(0.9)
            .excludedDomains(excludedDomains).build();
    }

    static AgentCapability capability(String name) {
        return AgentCapability.builder().name(name).qualityHint(0.9).build();
    }

    @Test
    void declared_excluded_domain_returns_excluded_status() {
        var descriptor = agent("agent1", capabilityWithExclusions("security-review", Set.of("rust", "go")));
        var status = health.probe(descriptor, "security-review", ProbeContext.of("rust"));
        assertThat(status).isInstanceOf(CapabilityStatus.Excluded.class);
        var excluded = (CapabilityStatus.Excluded) status;
        assertThat(excluded.domain()).isEqualTo("rust");
        assertThat(excluded.source()).isEqualTo(CapabilityStatus.ExclusionSource.DECLARED);
        assertThat(excluded.declineCount()).isEqualTo(0);
    }

    @Test
    void null_excluded_domains_does_not_throw_and_skips_step3() {
        var descriptor = agent("agent2", capability("code-review"));
        var status = health.probe(descriptor, "code-review", ProbeContext.of("rust"));
        assertThat(status).isInstanceOf(CapabilityStatus.Ready.class);
    }

    @Test
    void excluded_domain_not_in_set_returns_ready() {
        var descriptor = agent("agent3", capabilityWithExclusions("code-review", Set.of("rust")));
        var status = health.probe(descriptor, "code-review", ProbeContext.of("java"));
        assertThat(status).isInstanceOf(CapabilityStatus.Ready.class);
    }

    @Test
    void declared_exclusion_short_circuits_before_store() {
        // If declared exclusion works, store not consulted (store has no data — would return 0 → no exclusion if consulted)
        var descriptor = agent("agent4", capabilityWithExclusions("code-review", Set.of("rust")));
        var status = health.probe(descriptor, "code-review", ProbeContext.of("rust"));
        assertThat(status).isInstanceOf(CapabilityStatus.Excluded.class);
        assertThat(((CapabilityStatus.Excluded) status).source())
            .isEqualTo(CapabilityStatus.ExclusionSource.DECLARED);
    }

    @Test
    void learned_exclusion_at_default_threshold_returns_excluded() {
        specializationStore.setCount("agent5", "default", "code-review", "rust", SpecializationSignal.DECLINE, 3);
        var descriptor = agent("agent5", capability("code-review"));
        var status = health.probe(descriptor, "code-review", ProbeContext.of("rust"));
        assertThat(status).isInstanceOf(CapabilityStatus.Excluded.class);
        var excluded = (CapabilityStatus.Excluded) status;
        assertThat(excluded.domain()).isEqualTo("rust");
        assertThat(excluded.source()).isEqualTo(CapabilityStatus.ExclusionSource.LEARNED);
        assertThat(excluded.declineCount()).isEqualTo(3);
    }

    @Test
    void learned_exclusion_below_threshold_continues_to_ready() {
        specializationStore.setCount("agent6", "default", "code-review", "rust", SpecializationSignal.DECLINE, 2);
        var descriptor = agent("agent6", capability("code-review"));
        var status = health.probe(descriptor, "code-review", ProbeContext.of("rust"));
        assertThat(status).isInstanceOf(CapabilityStatus.Ready.class);
    }

    @Test
    void count_captured_in_single_call_matches_excluded_record() {
        specializationStore.setCount("agent7", "default", "code-review", "rust", SpecializationSignal.DECLINE, 5);
        var descriptor = agent("agent7", capability("code-review"));
        var excluded = (CapabilityStatus.Excluded) health.probe(descriptor, "code-review", ProbeContext.of("rust"));
        assertThat(excluded.declineCount()).isEqualTo(5);
    }

    @Test
    void null_task_domain_skips_both_exclusion_checks() {
        var descriptor = agent("agent8", capabilityWithExclusions("code-review", Set.of("rust")));
        specializationStore.setCount("agent8", "default", "code-review", "rust", SpecializationSignal.DECLINE, 10);
        var status = health.probe(descriptor, "code-review", ProbeContext.of(null));
        assertThat(status).isInstanceOf(CapabilityStatus.Ready.class);
    }

    @Test
    void threshold_resolved_from_preference_provider_when_satisfied() {
        var mockPreferences = mock(Preferences.class);
        var mockProvider = mock(PreferenceProvider.class);
        when(preferenceProviderInstance.isUnsatisfied()).thenReturn(false);
        when(preferenceProviderInstance.get()).thenReturn(mockProvider);
        when(mockProvider.resolve(any(SettingsScope.class))).thenReturn(mockPreferences);
        when(mockPreferences.getOrDefault(EidosPreferenceKeys.EXCLUDE_THRESHOLD))
            .thenReturn(new ExcludeThresholdPreference(2));

        specializationStore.setCount("agent9", "default", "code-review", "rust", SpecializationSignal.DECLINE, 2);
        var descriptor = agent("agent9", capability("code-review"));
        var status = health.probe(descriptor, "code-review", ProbeContext.of("rust"));

        assertThat(status).isInstanceOf(CapabilityStatus.Excluded.class);
    }

    @Test
    void no_preference_provider_falls_back_to_default_threshold_of_3() {
        // isUnsatisfied() = true (setUp default)
        specializationStore.setCount("agent10", "default", "code-review", "rust", SpecializationSignal.DECLINE, 2);
        var descriptor = agent("agent10", capability("code-review"));
        assertThat(health.probe(descriptor, "code-review", ProbeContext.of("rust")))
            .isInstanceOf(CapabilityStatus.Ready.class);  // 2 < 3

        specializationStore.setCount("agent10", "default", "code-review", "rust", SpecializationSignal.DECLINE, 3);
        assertThat(health.probe(descriptor, "code-review", ProbeContext.of("rust")))
            .isInstanceOf(CapabilityStatus.Excluded.class);  // 3 >= 3
    }

    @Test
    void success_data_does_not_affect_probe_result() {
        specializationStore.setCount("agent11", "default", "code-review", "rust", SpecializationSignal.SUCCESS, 10);
        var descriptor = agent("agent11", capability("code-review"));
        var status = health.probe(descriptor, "code-review", ProbeContext.of("rust"));
        assertThat(status).isInstanceOf(CapabilityStatus.Ready.class);
    }
}

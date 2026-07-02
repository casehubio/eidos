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

import io.casehub.eidos.api.BehavioralSignal;

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

    static class StubSpecializationStore implements BehavioralSignalStore {
        private final Map<String, Integer> counts = new HashMap<>();

        void setCount(String agentId, String tenancyId, String capability,
                       String domain, BehavioralSignal signal, int count) {
            counts.put(agentId + "|" + tenancyId + "|" + capability + "|" + domain + "|" + signal.name(), count);
        }

        @Override public void record(String a, String t, String c, String d, BehavioralSignal s) {}
        @Override public void clear(String a, String t, String c, BehavioralSignal s) {}
        @Override public Map<String, Integer> learned(String a, String t, String c, BehavioralSignal s) { return Map.of(); }

        @Override
        public int count(String agentId, String tenancyId, String capabilityName,
                          String domain, BehavioralSignal signal) {
            return counts.getOrDefault(
                agentId + "|" + tenancyId + "|" + capabilityName + "|" + domain + "|" + signal.name(), 0);
        }
    }

    @Mock
    @SuppressWarnings("unchecked")
    Instance<PreferenceProvider> preferenceProviderInstance;

    @Mock
    VocabularyRegistry mockVocabRegistry;

    StubStateStore stateStore;
    StubSpecializationStore specializationStore;
    DefaultCapabilityHealth health;

    @BeforeEach
    void setUp() {
        stateStore = new StubStateStore();
        specializationStore = new StubSpecializationStore();
        lenient().when(preferenceProviderInstance.isUnsatisfied()).thenReturn(true);
        health = new DefaultCapabilityHealth(0.3, stateStore, specializationStore, preferenceProviderInstance, mockVocabRegistry);
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
        specializationStore.setCount("agent5", "default", "code-review", "rust", BehavioralSignal.DECLINE, 3);
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
        specializationStore.setCount("agent6", "default", "code-review", "rust", BehavioralSignal.DECLINE, 2);
        var descriptor = agent("agent6", capability("code-review"));
        var status = health.probe(descriptor, "code-review", ProbeContext.of("rust"));
        assertThat(status).isInstanceOf(CapabilityStatus.Ready.class);
    }

    @Test
    void count_captured_in_single_call_matches_excluded_record() {
        specializationStore.setCount("agent7", "default", "code-review", "rust", BehavioralSignal.DECLINE, 5);
        var descriptor = agent("agent7", capability("code-review"));
        var excluded = (CapabilityStatus.Excluded) health.probe(descriptor, "code-review", ProbeContext.of("rust"));
        assertThat(excluded.declineCount()).isEqualTo(5);
    }

    @Test
    void null_task_domain_skips_both_exclusion_checks() {
        var descriptor = agent("agent8", capabilityWithExclusions("code-review", Set.of("rust")));
        specializationStore.setCount("agent8", "default", "code-review", "rust", BehavioralSignal.DECLINE, 10);
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

        specializationStore.setCount("agent9", "default", "code-review", "rust", BehavioralSignal.DECLINE, 2);
        var descriptor = agent("agent9", capability("code-review"));
        var status = health.probe(descriptor, "code-review", ProbeContext.of("rust"));

        assertThat(status).isInstanceOf(CapabilityStatus.Excluded.class);
    }

    @Test
    void no_preference_provider_falls_back_to_default_threshold_of_3() {
        // isUnsatisfied() = true (setUp default)
        specializationStore.setCount("agent10", "default", "code-review", "rust", BehavioralSignal.DECLINE, 2);
        var descriptor = agent("agent10", capability("code-review"));
        assertThat(health.probe(descriptor, "code-review", ProbeContext.of("rust")))
            .isInstanceOf(CapabilityStatus.Ready.class);  // 2 < 3

        specializationStore.setCount("agent10", "default", "code-review", "rust", BehavioralSignal.DECLINE, 3);
        assertThat(health.probe(descriptor, "code-review", ProbeContext.of("rust")))
            .isInstanceOf(CapabilityStatus.Excluded.class);  // 3 >= 3
    }

    @Test
    void success_data_does_not_affect_probe_result() {
        specializationStore.setCount("agent11", "default", "code-review", "rust", BehavioralSignal.SUCCESS, 10);
        var descriptor = agent("agent11", capability("code-review"));
        var status = health.probe(descriptor, "code-review", ProbeContext.of("rust"));
        assertThat(status).isInstanceOf(CapabilityStatus.Ready.class);
    }

    @Test
    void learned_exclusion_uses_declared_name_under_subsumption() {
        // Agent declares "security-code-review" grounded. Probe for "code-review" (parent).
        // DECLINE signals recorded against "security-code-review" (declared name).
        // Mock vocab: code-review → security-code-review is Specialization(1)
        var cap = AgentCapability.builder().name("security-code-review")
            .capabilityVocabulary("urn:test:cap").qualityHint(0.9).build();
        var descriptor = agent("agent-sub1", cap);

        // No exact match for "code-review", so CapabilityResolver will try subsumption
        when(mockVocabRegistry.match("urn:test:cap", "security-code-review", "code-review"))
            .thenReturn(new MatchDegree.Specialization(1));

        // Record declines against declared name "security-code-review"
        specializationStore.setCount("agent-sub1", "default", "security-code-review",
            "rust", BehavioralSignal.DECLINE, 3);

        var status = health.probe(descriptor, "code-review", ProbeContext.of("rust"));
        assertThat(status).isInstanceOf(CapabilityStatus.Excluded.class);
        var excluded = (CapabilityStatus.Excluded) status;
        assertThat(excluded.source()).isEqualTo(CapabilityStatus.ExclusionSource.LEARNED);
        assertThat(excluded.declineCount()).isEqualTo(3);
    }

    @Test
    void learned_exclusion_invisible_under_wrong_key() {
        // Same setup, but DECLINE recorded against "code-review" (query tag, wrong key)
        var cap = AgentCapability.builder().name("security-code-review")
            .capabilityVocabulary("urn:test:cap").qualityHint(0.9).build();
        var descriptor = agent("agent-sub2", cap);

        when(mockVocabRegistry.match("urn:test:cap", "security-code-review", "code-review"))
            .thenReturn(new MatchDegree.Specialization(1));

        // Record against query tag — wrong key
        specializationStore.setCount("agent-sub2", "default", "code-review",
            "rust", BehavioralSignal.DECLINE, 3);

        var status = health.probe(descriptor, "code-review", ProbeContext.of("rust"));
        // Should be Ready — declines under wrong key are invisible
        assertThat(status).isInstanceOf(CapabilityStatus.Ready.class);
    }

    @Test
    void learned_exclusion_under_plugin_direction() {
        // Agent declares "code-review" (general, grounded). Probe for "security-code-review" (child).
        var cap = AgentCapability.builder().name("code-review")
            .capabilityVocabulary("urn:test:cap").qualityHint(0.9).build();
        var descriptor = agent("agent-sub3", cap);

        when(mockVocabRegistry.match("urn:test:cap", "code-review", "security-code-review"))
            .thenReturn(new MatchDegree.Plugin(1));

        // Record declines against declared name "code-review"
        specializationStore.setCount("agent-sub3", "default", "code-review",
            "rust", BehavioralSignal.DECLINE, 3);

        var status = health.probe(descriptor, "security-code-review", ProbeContext.of("rust"));
        assertThat(status).isInstanceOf(CapabilityStatus.Excluded.class);
        var excluded = (CapabilityStatus.Excluded) status;
        assertThat(excluded.source()).isEqualTo(CapabilityStatus.ExclusionSource.LEARNED);
    }

    @Test
    void learned_exclusion_exact_match_regression() {
        // Exact match: declared and query tag are the same — regression guard
        var descriptor = agent("agent-sub4", capability("code-review"));

        specializationStore.setCount("agent-sub4", "default", "code-review",
            "rust", BehavioralSignal.DECLINE, 3);

        var status = health.probe(descriptor, "code-review", ProbeContext.of("rust"));
        assertThat(status).isInstanceOf(CapabilityStatus.Excluded.class);
        assertThat(((CapabilityStatus.Excluded) status).declineCount()).isEqualTo(3);
    }
}

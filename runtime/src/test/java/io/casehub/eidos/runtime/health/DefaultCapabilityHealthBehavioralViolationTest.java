package io.casehub.eidos.runtime.health;

import io.casehub.eidos.api.*;
import io.casehub.eidos.api.CapabilityHealth.CapabilityStatus;
import io.casehub.eidos.api.CapabilityHealth.ProbeContext;
import io.casehub.platform.api.preferences.PreferenceProvider;
import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DefaultCapabilityHealthBehavioralViolationTest {

    StubBehavioralSignalStore signalStore;
    DefaultCapabilityHealth health;

    @BeforeEach
    void setUp() {
        signalStore = new StubBehavioralSignalStore();
        @SuppressWarnings("unchecked")
        Instance<PreferenceProvider> emptyProvider = mock(Instance.class);
        when(emptyProvider.isUnsatisfied()).thenReturn(true);
        health = new DefaultCapabilityHealth(0.3, mock(AgentStateStore.class),
                signalStore, emptyProvider, new StubVocabularyRegistry());
    }

    private AgentDescriptor agent(String id, String capabilityName) {
        return AgentDescriptor.builder()
                .agentId(id).name("Test").slot("reviewer").tenancyId("default")
                .capabilities(List.of(
                        AgentCapability.builder().name(capabilityName).build()))
                .build();
    }

    @Test
    void violations_below_threshold_returns_ready() {
        signalStore.setViolations("a1", "default", "code-review",
                Map.of("latency", 2));
        var status = health.probe(agent("a1", "code-review"), "code-review",
                ProbeContext.of("java"));
        assertThat(status).isInstanceOf(CapabilityStatus.Ready.class);
    }

    @Test
    void violations_at_threshold_returns_behavioral_violation() {
        signalStore.setViolations("a1", "default", "code-review",
                Map.of("latency", 3));
        var status = health.probe(agent("a1", "code-review"), "code-review",
                ProbeContext.of("java"));
        assertThat(status).isInstanceOf(CapabilityStatus.BehavioralViolation.class);
        var violation = (CapabilityStatus.BehavioralViolation) status;
        assertThat(violation.violations()).containsEntry("latency", 3);
    }

    @Test
    void multiple_dimensions_above_threshold_all_returned() {
        signalStore.setViolations("a1", "default", "code-review",
                Map.of("latency", 5, "attestation-rate", 4));
        var status = health.probe(agent("a1", "code-review"), "code-review",
                ProbeContext.of("java"));
        assertThat(status).isInstanceOf(CapabilityStatus.BehavioralViolation.class);
        var violation = (CapabilityStatus.BehavioralViolation) status;
        assertThat(violation.violations()).hasSize(2)
                .containsEntry("latency", 5)
                .containsEntry("attestation-rate", 4);
    }

    @Test
    void mixed_dimensions_only_above_threshold_returned() {
        signalStore.setViolations("a1", "default", "code-review",
                Map.of("latency", 5, "attestation-rate", 1));
        var status = health.probe(agent("a1", "code-review"), "code-review",
                ProbeContext.of("java"));
        assertThat(status).isInstanceOf(CapabilityStatus.BehavioralViolation.class);
        var violation = (CapabilityStatus.BehavioralViolation) status;
        assertThat(violation.violations()).hasSize(1).containsEntry("latency", 5);
    }

    @Test
    void null_task_domain_still_checks_compliance() {
        signalStore.setViolations("a1", "default", "code-review",
                Map.of("latency", 3));
        var status = health.probe(agent("a1", "code-review"), "code-review",
                ProbeContext.of(null));
        assertThat(status).isInstanceOf(CapabilityStatus.BehavioralViolation.class);
    }

    @Test
    void no_violations_recorded_returns_ready() {
        var status = health.probe(agent("a1", "code-review"), "code-review",
                ProbeContext.of("java"));
        assertThat(status).isInstanceOf(CapabilityStatus.Ready.class);
    }

    @Test
    void uses_resolved_capability_name_not_query_tag() {
        signalStore.setViolations("a1", "default", "code-review",
                Map.of("latency", 3));
        var status = health.probe(agent("a1", "code-review"), "review",
                ProbeContext.of("java"));
        // "review" doesn't match "code-review" exactly — agent has no "review" capability
        // so probe returns Unavailable before reaching Step 6
        assertThat(status).isInstanceOf(CapabilityStatus.Unavailable.class);
    }

    // --- Stubs ---

    static class StubBehavioralSignalStore implements BehavioralSignalStore {
        private final Map<String, Map<String, Integer>> violationData = new java.util.HashMap<>();

        void setViolations(String agentId, String tenancyId, String capabilityName,
                           Map<String, Integer> violations) {
            violationData.put(agentId + "|" + tenancyId + "|" + capabilityName, violations);
        }

        @Override public void record(String a, String t, String c, String q, BehavioralSignal s) {}
        @Override public void clear(String a, String t, String c, BehavioralSignal s) {}
        @Override public Map<String, Integer> learned(String a, String t, String c, BehavioralSignal s) {
            if (s != BehavioralSignal.VIOLATED) return Map.of();
            return violationData.getOrDefault(a + "|" + t + "|" + c, Map.of());
        }
        @Override public int count(String a, String t, String c, String q, BehavioralSignal s) { return 0; }
    }

    static class StubVocabularyRegistry implements VocabularyRegistry {
        @Override public <T extends Enum<T> & VocabularyTerm> void register(Class<T> c) {}
        @Override public boolean isRegistered(String uri) { return false; }
        @Override public java.util.Optional<? extends VocabularyTerm> resolve(String uri, String v) { return java.util.Optional.empty(); }
        @Override public java.util.List<? extends VocabularyTerm> allTerms(String uri) { return List.of(); }
        @Override public java.util.Optional<String> equivalentValues(String fromUri, String value, String toUri) { return java.util.Optional.empty(); }
        @Override public java.util.Optional<String> equivalentValues(String fromUri, String value, String toUri, DispositionAxis axis) { return java.util.Optional.empty(); }
        @Override public <T extends Enum<T> & VocabularyTerm> java.util.Optional<T> resolve(Class<T> vocab, String value) { return java.util.Optional.empty(); }
        @Override public <S extends Enum<S> & VocabularyTerm, T extends Enum<T> & VocabularyTerm> java.util.Optional<T> equivalentValues(S from, Class<T> targetVocab) { return java.util.Optional.empty(); }
        @Override public <S extends Enum<S> & VocabularyTerm, T extends Enum<T> & VocabularyTerm> java.util.Optional<T> equivalentValues(S from, Class<T> targetVocab, DispositionAxis axis) { return java.util.Optional.empty(); }
        @Override public java.util.Optional<VocabularyMetadata> vocabularyMetadata(String uri) { return java.util.Optional.empty(); }
        @Override public boolean subsumes(String uri, String ancestor, String descendant) { return false; }
        @Override public MatchDegree match(String uri, String registered, String query) { return new MatchDegree.None(); }
        @Override public java.util.List<? extends VocabularyTerm> ancestors(String uri, String term) { return List.of(); }
        @Override public java.util.List<? extends VocabularyTerm> descendants(String uri, String term) { return List.of(); }
        @Override public Map<String, java.util.Set<String>> expandForMatchingByVocabulary(String termName) { return Map.of(); }
    }
}

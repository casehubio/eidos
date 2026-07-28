package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;

class BehavioralExpectationsTest {

    // --- Inner enum for testing ---
    @VocabularyMetadata(uri = "urn:test:autonomy", name = "Test Autonomy", version = "1.0")
    enum TestAutonomyTerm implements VocabularyTerm {
        SUPERVISED("supervised", "Supervised", true),
        BOUNDED("bounded", "Bounded", true),
        SELF_GOVERNING("self-governing", "Self-Governing", false);

        private final String value, label;
        private final boolean supervised;

        TestAutonomyTerm(String value, String label, boolean supervised) {
            this.value = value;
            this.label = label;
            this.supervised = supervised;
        }

        @Override public String value() { return value; }
        @Override public String label() { return label; }
        @Override public boolean impliesSupervision() { return supervised; }
    }

    private static final String TEST_VOCAB_URI = "urn:test:autonomy";

    private static VocabularyRegistry testRegistry() {
        return new VocabularyRegistry() {
            @Override public <T extends Enum<T> & VocabularyTerm> void register(Class<T> vocab) {}
            @Override public boolean isRegistered(String vocabUri) {
                return "urn:test:autonomy".equals(vocabUri);
            }
            @Override public Optional<? extends VocabularyTerm> resolve(String vocabUri, String value) {
                if (!"urn:test:autonomy".equals(vocabUri)) return Optional.empty();
                for (TestAutonomyTerm t : TestAutonomyTerm.values()) {
                    if (t.value().equals(value)) return Optional.of(t);
                }
                return Optional.empty();
            }
            @Override public List<? extends VocabularyTerm> allTerms(String vocabUri) { return List.of(); }
            @Override public Optional<String> equivalentValues(String f, String v, String t) { return Optional.empty(); }
            @Override public Optional<String> equivalentValues(String f, String v, String t, DispositionAxis a) { return Optional.empty(); }
            @Override public <T extends Enum<T> & VocabularyTerm> Optional<T> resolve(Class<T> vocab, String value) { return Optional.empty(); }
            @Override public <S extends Enum<S> & VocabularyTerm, T extends Enum<T> & VocabularyTerm> Optional<T> equivalentValues(S from, Class<T> targetVocab) { return Optional.empty(); }
            @Override public <S extends Enum<S> & VocabularyTerm, T extends Enum<T> & VocabularyTerm> Optional<T> equivalentValues(S from, Class<T> targetVocab, DispositionAxis axis) { return Optional.empty(); }
            @Override public Optional<VocabularyMetadata> vocabularyMetadata(String uri) { return Optional.empty(); }
            @Override public boolean subsumes(String vocabUri, String generalValue, String specificValue) { return false; }
            @Override public MatchDegree match(String vocabUri, String declaredValue, String requestedValue) { return new MatchDegree.None(); }
            @Override public List<? extends VocabularyTerm> ancestors(String vocabUri, String value) { return List.of(); }
            @Override public List<? extends VocabularyTerm> descendants(String vocabUri, String value) { return List.of(); }
            @Override public java.util.Map<String, java.util.Set<String>> expandForMatchingByVocabulary(String value) { return java.util.Map.of(); }
            @Override public java.util.Set<String> registeredUris() { return java.util.Set.of(TEST_VOCAB_URI); }
        };
    }

    @Test
    void latencyBound_returns_hint_when_present() {
        var cap = AgentCapability.builder().name("code-review")
                .latencyHintP50Ms(5000L).build();
        assertThat(BehavioralExpectations.latencyBound(cap)).hasValue(5000L);
    }

    @Test
    void latencyBound_empty_when_no_hint() {
        var cap = AgentCapability.builder().name("code-review").build();
        assertThat(BehavioralExpectations.latencyBound(cap)).isEmpty();
    }

    @Test
    void delegationExpected_true_when_delegation_flag_set() {
        var disp = AgentDisposition.builder().delegation(true).build();
        assertThat(BehavioralExpectations.delegationExpected(disp)).isTrue();
    }

    @Test
    void delegationExpected_false_when_delegation_not_set() {
        var disp = AgentDisposition.builder().build();
        assertThat(BehavioralExpectations.delegationExpected(disp)).isFalse();
    }

    @Test
    void delegationExpected_false_when_null_disposition() {
        assertThat(BehavioralExpectations.delegationExpected(null)).isFalse();
    }

    @Test
    void escalationExpected_true_for_supervised_term() {
        var disp = AgentDisposition.builder().autonomy("supervised").build();
        assertThat(BehavioralExpectations.escalationExpected(disp, TEST_VOCAB_URI, testRegistry())).isTrue();
    }

    @Test
    void escalationExpected_true_for_bounded_term() {
        var disp = AgentDisposition.builder().autonomy("bounded").build();
        assertThat(BehavioralExpectations.escalationExpected(disp, TEST_VOCAB_URI, testRegistry())).isTrue();
    }

    @Test
    void escalationExpected_false_for_self_governing_term() {
        var disp = AgentDisposition.builder().autonomy("self-governing").build();
        assertThat(BehavioralExpectations.escalationExpected(disp, TEST_VOCAB_URI, testRegistry())).isFalse();
    }

    @Test
    void escalationExpected_false_when_null_disposition() {
        assertThat(BehavioralExpectations.escalationExpected((AgentDisposition) null, TEST_VOCAB_URI, testRegistry())).isFalse();
    }

    @Test
    void escalationExpected_false_when_null_autonomy() {
        var disp = AgentDisposition.builder().build();
        assertThat(BehavioralExpectations.escalationExpected(disp, TEST_VOCAB_URI, testRegistry())).isFalse();
    }

    @Test
    void escalationExpected_false_when_null_vocabUri() {
        var disp = AgentDisposition.builder().autonomy("supervised").build();
        assertThat(BehavioralExpectations.escalationExpected(disp, null, testRegistry())).isFalse();
    }

    @Test
    void escalationExpected_false_when_null_registry() {
        var disp = AgentDisposition.builder().autonomy("supervised").build();
        assertThat(BehavioralExpectations.escalationExpected(disp, TEST_VOCAB_URI, null)).isFalse();
    }

    @Test
    void escalationExpected_false_when_value_unresolvable() {
        var disp = AgentDisposition.builder().autonomy("unknown-value").build();
        assertThat(BehavioralExpectations.escalationExpected(disp, TEST_VOCAB_URI, testRegistry())).isFalse();
    }

    @Test
    void escalationExpected_convenience_null_descriptor() {
        assertThat(BehavioralExpectations.escalationExpected((AgentDescriptor) null, testRegistry())).isFalse();
    }

    @Test
    void escalationExpected_convenience_null_disposition() {
        var desc = AgentDescriptor.builder()
                .agentId("a1").name("Agent").slot("worker").tenancyId("t1")
                .build();
        assertThat(BehavioralExpectations.escalationExpected(desc, testRegistry())).isFalse();
    }

    @Test
    void escalationExpected_convenience_no_vocab_uri() {
        var desc = AgentDescriptor.builder()
                .agentId("a1").name("Agent").slot("worker").tenancyId("t1")
                .disposition(AgentDisposition.builder().autonomy("supervised").build())
                .build();
        assertThat(BehavioralExpectations.escalationExpected(desc, testRegistry())).isFalse();
    }

    @Test
    void escalationExpected_convenience_with_domain_vocab() {
        var desc = AgentDescriptor.builder()
                .agentId("a1").name("Agent").slot("worker").tenancyId("t1")
                .domainVocabulary(TEST_VOCAB_URI)
                .disposition(AgentDisposition.builder().autonomy("supervised").build())
                .build();
        assertThat(BehavioralExpectations.escalationExpected(desc, testRegistry())).isTrue();
    }

    @Test
    void escalationExpected_convenience_autonomous_with_domain_vocab() {
        var desc = AgentDescriptor.builder()
                .agentId("a1").name("Agent").slot("worker").tenancyId("t1")
                .domainVocabulary(TEST_VOCAB_URI)
                .disposition(AgentDisposition.builder().autonomy("self-governing").build())
                .build();
        assertThat(BehavioralExpectations.escalationExpected(desc, testRegistry())).isFalse();
    }
}

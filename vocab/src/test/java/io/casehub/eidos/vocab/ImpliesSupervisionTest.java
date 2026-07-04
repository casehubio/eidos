package io.casehub.eidos.vocab;

import io.casehub.eidos.api.AgentDisposition;
import io.casehub.eidos.api.BehavioralExpectations;
import io.casehub.eidos.api.DispositionAxis;
import io.casehub.eidos.api.MatchDegree;
import io.casehub.eidos.api.VocabularyMetadata;
import io.casehub.eidos.api.VocabularyRegistry;
import io.casehub.eidos.api.VocabularyTerm;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ImpliesSupervisionTest {

    // --- ConscientiousnessTerm: AUTONOMY axis ---

    @Test
    void conscientiousness_directed_implies_supervision() {
        assertThat(ConscientiousnessTerm.DIRECTED.impliesSupervision()).isTrue();
    }

    @Test
    void conscientiousness_semi_autonomous_implies_supervision() {
        assertThat(ConscientiousnessTerm.SEMI_AUTONOMOUS.impliesSupervision()).isTrue();
    }

    @Test
    void conscientiousness_autonomous_does_not_imply_supervision() {
        assertThat(ConscientiousnessTerm.AUTONOMOUS.impliesSupervision()).isFalse();
    }

    // --- ConscientiousnessTerm: non-AUTONOMY axes default to false ---

    @Test
    void conscientiousness_rule_following_terms_do_not_imply_supervision() {
        assertThat(ConscientiousnessTerm.STRICT.impliesSupervision()).isFalse();
        assertThat(ConscientiousnessTerm.PRINCIPLED.impliesSupervision()).isFalse();
        assertThat(ConscientiousnessTerm.FLEXIBLE.impliesSupervision()).isFalse();
    }

    @Test
    void conscientiousness_risk_appetite_terms_do_not_imply_supervision() {
        assertThat(ConscientiousnessTerm.CONSERVATIVE.impliesSupervision()).isFalse();
        assertThat(ConscientiousnessTerm.MEASURED.impliesSupervision()).isFalse();
        assertThat(ConscientiousnessTerm.BOLD.impliesSupervision()).isFalse();
    }

    @Test
    void conscientiousness_social_orientation_terms_do_not_imply_supervision() {
        assertThat(ConscientiousnessTerm.COLLABORATIVE.impliesSupervision()).isFalse();
        assertThat(ConscientiousnessTerm.INDEPENDENT.impliesSupervision()).isFalse();
        assertThat(ConscientiousnessTerm.FACILITATIVE.impliesSupervision()).isFalse();
    }

    // --- DiscTerm ---

    @Test
    void disc_steadiness_implies_supervision() {
        assertThat(DiscTerm.STEADINESS.impliesSupervision()).isTrue();
    }

    @Test
    void disc_influence_implies_supervision() {
        assertThat(DiscTerm.INFLUENCE.impliesSupervision()).isTrue();
    }

    @Test
    void disc_conscientiousness_disc_implies_supervision() {
        assertThat(DiscTerm.CONSCIENTIOUSNESS_DISC.impliesSupervision()).isTrue();
    }

    @Test
    void disc_dominance_does_not_imply_supervision() {
        assertThat(DiscTerm.DOMINANCE.impliesSupervision()).isFalse();
    }

    // --- Cross-vocabulary consistency ---

    @Test
    void disc_autonomy_equivalents_have_consistent_supervision() {
        for (DiscTerm disc : DiscTerm.values()) {
            Optional<VocabularyTerm> equivalent =
                    disc.axisExactMatch(ConscientiousnessTerm.class, DispositionAxis.AUTONOMY);
            if (equivalent.isPresent()) {
                assertThat(disc.impliesSupervision())
                        .as("%s.impliesSupervision() should match its ConscientiousnessTerm AUTONOMY equivalent %s",
                                disc, equivalent.get())
                        .isEqualTo(equivalent.get().impliesSupervision());
            }
        }
    }

    // --- Cross-vocabulary escalation expectations ---

    @Test
    void escalationExpected_with_disc_steadiness() {
        var disp = AgentDisposition.builder().autonomy("steadiness").build();
        assertThat(BehavioralExpectations.escalationExpected(disp, DiscTerm.URI, discRegistry())).isTrue();
    }

    @Test
    void escalationExpected_with_disc_dominance() {
        var disp = AgentDisposition.builder().autonomy("dominance").build();
        assertThat(BehavioralExpectations.escalationExpected(disp, DiscTerm.URI, discRegistry())).isFalse();
    }

    private static VocabularyRegistry discRegistry() {
        return new VocabularyRegistry() {
            @Override public <T extends Enum<T> & VocabularyTerm> void register(Class<T> vocab) {}
            @Override public boolean isRegistered(String vocabUri) {
                return DiscTerm.URI.equals(vocabUri);
            }
            @Override public Optional<? extends VocabularyTerm> resolve(String vocabUri, String value) {
                if (!DiscTerm.URI.equals(vocabUri)) return Optional.empty();
                for (DiscTerm t : DiscTerm.values()) {
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
        };
    }
}

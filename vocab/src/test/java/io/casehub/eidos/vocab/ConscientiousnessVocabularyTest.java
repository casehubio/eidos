package io.casehub.eidos.vocab;

import io.casehub.eidos.api.VocabularyTerm;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ConscientiousnessVocabularyTest {

    @Test
    void uri_constant_is_correct() {
        assertThat(ConscientiousnessTerm.URI).isEqualTo("urn:casehub:vocab:conscientiousness");
    }

    @Test
    void covers_all_twelve_terms() {
        assertThat(ConscientiousnessTerm.values()).hasSize(12);
    }

    @Test
    void rule_following_terms_present() {
        assertThat(ConscientiousnessTerm.STRICT.value()).isEqualTo("strict");
        assertThat(ConscientiousnessTerm.PRINCIPLED.value()).isEqualTo("principled");
        assertThat(ConscientiousnessTerm.FLEXIBLE.value()).isEqualTo("flexible");
    }

    @Test
    void risk_appetite_terms_present() {
        assertThat(ConscientiousnessTerm.CONSERVATIVE.value()).isEqualTo("conservative");
        assertThat(ConscientiousnessTerm.MEASURED.value()).isEqualTo("measured");
        assertThat(ConscientiousnessTerm.BOLD.value()).isEqualTo("bold");
    }

    @Test
    void social_orientation_terms_present() {
        assertThat(ConscientiousnessTerm.COLLABORATIVE.value()).isEqualTo("collaborative");
        assertThat(ConscientiousnessTerm.INDEPENDENT.value()).isEqualTo("independent");
        assertThat(ConscientiousnessTerm.FACILITATIVE.value()).isEqualTo("facilitative");
    }

    @Test
    void autonomy_terms_present() {
        assertThat(ConscientiousnessTerm.DIRECTED.value()).isEqualTo("directed");
        assertThat(ConscientiousnessTerm.SEMI_AUTONOMOUS.value()).isEqualTo("semi-autonomous");
        assertThat(ConscientiousnessTerm.AUTONOMOUS.value()).isEqualTo("autonomous");
    }

    @Test
    void strict_has_aliases() {
        assertThat(ConscientiousnessTerm.STRICT.aliases()).contains("rule-bound", "compliant");
    }

    @Test
    void bold_description_is_non_empty() {
        assertThat(ConscientiousnessTerm.BOLD.description()).isNotEmpty();
    }

    @Test
    void vocabUri_from_annotation_matches_uri_constant() {
        var meta = ConscientiousnessTerm.class.getAnnotation(
            io.casehub.eidos.api.VocabularyMetadata.class);
        assertThat(meta.uri()).isEqualTo(ConscientiousnessTerm.URI);
    }

    @Test
    void exactMatch_returns_empty_for_all_terms() {
        for (ConscientiousnessTerm t : ConscientiousnessTerm.values()) {
            assertThat(t.exactMatch(Object.class)).isEmpty();
        }
    }
}

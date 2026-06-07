package io.casehub.eidos.vocab;

import io.casehub.eidos.api.DispositionAxis;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class BelbinVocabularyTest {

    @Test
    void uri_constant_is_correct() {
        assertThat(BelbinTerm.URI).isEqualTo("urn:casehub:vocab:belbin");
    }

    @Test
    void has_nine_roles() {
        assertThat(BelbinTerm.values()).hasSize(9);
    }

    @Test
    void plant_value_and_alias() {
        assertThat(BelbinTerm.PLANT.value()).isEqualTo("plant");
        assertThat(BelbinTerm.PLANT.aliases()).containsExactly("pl");
    }

    @Test
    void co_ordinator_value_and_alias() {
        assertThat(BelbinTerm.CO_ORDINATOR.value()).isEqualTo("co-ordinator");
        assertThat(BelbinTerm.CO_ORDINATOR.aliases()).containsExactly("co");
    }

    @Test
    void completer_finisher_value_and_alias() {
        assertThat(BelbinTerm.COMPLETER_FINISHER.value()).isEqualTo("completer-finisher");
        assertThat(BelbinTerm.COMPLETER_FINISHER.aliases()).containsExactly("cf");
    }

    @Test
    void vocabUri_from_annotation_matches_uri_constant() {
        var meta = BelbinTerm.class.getAnnotation(
            io.casehub.eidos.api.VocabularyMetadata.class);
        assertThat(meta.uri()).isEqualTo(BelbinTerm.URI);
    }

    @Test
    void all_aliases_present() {
        assertThat(BelbinTerm.PLANT.aliases()).containsExactly("pl");
        assertThat(BelbinTerm.RESOURCE_INVESTIGATOR.aliases()).containsExactly("ri");
        assertThat(BelbinTerm.CO_ORDINATOR.aliases()).containsExactly("co");
        assertThat(BelbinTerm.SHAPER.aliases()).containsExactly("sh");
        assertThat(BelbinTerm.MONITOR_EVALUATOR.aliases()).containsExactly("me");
        assertThat(BelbinTerm.TEAMWORKER.aliases()).containsExactly("tw");
        assertThat(BelbinTerm.IMPLEMENTER.aliases()).containsExactly("imp");
        assertThat(BelbinTerm.COMPLETER_FINISHER.aliases()).containsExactly("cf");
        assertThat(BelbinTerm.SPECIALIST.aliases()).containsExactly("sp");
    }

    @Test
    void exactMatch_returns_empty_for_all_terms() {
        for (BelbinTerm t : BelbinTerm.values()) {
            assertThat(t.exactMatch(Object.class)).isEmpty();
        }
    }

    @Test
    void axisExactMatch_returns_empty_for_all_terms_and_all_axes() {
        for (BelbinTerm t : BelbinTerm.values()) {
            for (DispositionAxis axis : DispositionAxis.values()) {
                assertThat(t.axisExactMatch(Object.class, axis)).isEmpty();
            }
        }
    }

    @Test
    void descriptions_are_non_empty() {
        for (BelbinTerm t : BelbinTerm.values()) {
            assertThat(t.description()).isNotEmpty();
        }
    }
}

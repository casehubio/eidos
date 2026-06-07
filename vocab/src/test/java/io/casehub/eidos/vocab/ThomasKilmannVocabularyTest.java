package io.casehub.eidos.vocab;

import io.casehub.eidos.api.DispositionAxis;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ThomasKilmannVocabularyTest {

    @Test
    void uri_constant_is_correct() {
        assertThat(ThomasKilmannTerm.URI).isEqualTo("urn:casehub:vocab:thomas-kilmann");
    }

    @Test
    void has_five_terms() {
        assertThat(ThomasKilmannTerm.values()).hasSize(5);
    }

    @Test
    void competing_value_and_alias() {
        assertThat(ThomasKilmannTerm.COMPETING.value()).isEqualTo("competing");
        assertThat(ThomasKilmannTerm.COMPETING.aliases()).contains("competitive");
    }

    @Test
    void collaborating_value_and_alias() {
        assertThat(ThomasKilmannTerm.COLLABORATING.value()).isEqualTo("collaborating");
        assertThat(ThomasKilmannTerm.COLLABORATING.aliases()).contains("cooperative");
    }

    @Test
    void compromising_value_and_alias() {
        assertThat(ThomasKilmannTerm.COMPROMISING.value()).isEqualTo("compromising");
        assertThat(ThomasKilmannTerm.COMPROMISING.aliases()).contains("compromise");
    }

    @Test
    void avoiding_value_and_alias() {
        assertThat(ThomasKilmannTerm.AVOIDING.value()).isEqualTo("avoiding");
        assertThat(ThomasKilmannTerm.AVOIDING.aliases()).contains("avoidant");
    }

    @Test
    void accommodating_value_and_alias() {
        assertThat(ThomasKilmannTerm.ACCOMMODATING.value()).isEqualTo("accommodating");
        assertThat(ThomasKilmannTerm.ACCOMMODATING.aliases()).contains("deferring");
    }

    @Test
    void vocabUri_from_annotation_matches_uri_constant() {
        var meta = ThomasKilmannTerm.class.getAnnotation(
            io.casehub.eidos.api.VocabularyMetadata.class);
        assertThat(meta.uri()).isEqualTo(ThomasKilmannTerm.URI);
    }

    @Test
    void axisExactMatch_returns_empty_for_all_terms_and_any_target() {
        for (ThomasKilmannTerm t : ThomasKilmannTerm.values()) {
            for (DispositionAxis axis : DispositionAxis.values()) {
                assertThat(t.axisExactMatch(Object.class, axis)).isEmpty();
            }
        }
    }

    @Test
    void descriptions_are_non_empty() {
        for (ThomasKilmannTerm t : ThomasKilmannTerm.values()) {
            assertThat(t.description()).isNotEmpty();
        }
    }
}

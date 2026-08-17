package io.casehub.eidos.vocab;

import io.casehub.eidos.api.DispositionAxis;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class Sarc7VocabularyTest {

    @Test
    void uri_constant_is_correct() {
        assertThat(Sarc7Term.URI).isEqualTo("urn:casehub:vocab:sarc7");
    }

    @Test
    void has_seven_terms() {
        assertThat(Sarc7Term.values()).hasSize(7);
    }

    @Test
    void deadpan_has_correct_value_and_label() {
        assertThat(Sarc7Term.DEADPAN.value()).isEqualTo("deadpan");
        assertThat(Sarc7Term.DEADPAN.label()).isEqualTo("Deadpan");
    }

    @Test
    void all_values_are_unique() {
        var values = java.util.Arrays.stream(Sarc7Term.values())
                .map(Sarc7Term::value)
                .toList();
        assertThat(values).doesNotHaveDuplicates();
    }

    @Test
    void deadpan_dimensions_are_paper_derived() {
        assertThat(Sarc7Term.DEADPAN.incongruity()).isEqualTo(0.8);
        assertThat(Sarc7Term.DEADPAN.shockValue()).isEqualTo(0.2);
        assertThat(Sarc7Term.DEADPAN.contextDependency()).isEqualTo(0.6);
        assertThat(Sarc7Term.DEADPAN.emotionalTone()).isEqualTo(0.4);
    }

    @Test
    void obnoxious_dimensions_are_paper_derived() {
        assertThat(Sarc7Term.OBNOXIOUS.incongruity()).isEqualTo(0.6);
        assertThat(Sarc7Term.OBNOXIOUS.shockValue()).isEqualTo(0.8);
    }

    @Test
    void all_terms_have_non_empty_responseStyleGuidance() {
        for (Sarc7Term t : Sarc7Term.values()) {
            assertThat(t.responseStyleGuidance())
                .as("responseStyleGuidance for %s", t.name())
                .isNotBlank();
        }
    }

    @Test
    void all_terms_have_non_empty_antiPatternWarning() {
        for (Sarc7Term t : Sarc7Term.values()) {
            assertThat(t.antiPatternWarning())
                .as("antiPatternWarning for %s", t.name())
                .isNotBlank();
        }
    }

    @Test
    void deadpan_maps_to_conscientiousness_independent_on_social_orientation() {
        assertThat(Sarc7Term.DEADPAN.axisExactMatch(ConscientiousnessTerm.class, DispositionAxis.SOCIAL_ORIENTATION))
            .hasValue(ConscientiousnessTerm.INDEPENDENT);
    }

    @Test
    void deadpan_maps_to_tk_avoiding_on_conflict_mode() {
        assertThat(Sarc7Term.DEADPAN.axisExactMatch(ThomasKilmannTerm.class, DispositionAxis.CONFLICT_MODE))
            .hasValue(ThomasKilmannTerm.AVOIDING);
    }

    @Test
    void obnoxious_maps_to_tk_competing_on_conflict_mode() {
        assertThat(Sarc7Term.OBNOXIOUS.axisExactMatch(ThomasKilmannTerm.class, DispositionAxis.CONFLICT_MODE))
            .hasValue(ThomasKilmannTerm.COMPETING);
    }

    @Test
    void manic_maps_to_tk_collaborating_on_conflict_mode() {
        assertThat(Sarc7Term.MANIC.axisExactMatch(ThomasKilmannTerm.class, DispositionAxis.CONFLICT_MODE))
            .hasValue(ThomasKilmannTerm.COLLABORATING);
    }

    @Test
    void polite_maps_to_conscientiousness_directed_on_autonomy() {
        assertThat(Sarc7Term.POLITE.axisExactMatch(ConscientiousnessTerm.class, DispositionAxis.AUTONOMY))
            .hasValue(ConscientiousnessTerm.DIRECTED);
    }

    @Test
    void unknown_target_vocab_returns_empty() {
        for (Sarc7Term t : Sarc7Term.values()) {
            for (DispositionAxis axis : DispositionAxis.values()) {
                assertThat(t.axisExactMatch(SvoTerm.class, axis)).isEmpty();
            }
        }
    }

    @Test
    void no_term_implies_supervision() {
        for (Sarc7Term t : Sarc7Term.values()) {
            assertThat(t.impliesSupervision())
                .as("impliesSupervision for %s", t.name())
                .isFalse();
        }
    }

    @Test
    void dimensions_are_within_valid_range() {
        for (Sarc7Term t : Sarc7Term.values()) {
            assertThat(t.incongruity()).as("incongruity for %s", t.name()).isBetween(0.0, 1.0);
            assertThat(t.shockValue()).as("shockValue for %s", t.name()).isBetween(0.0, 1.0);
            assertThat(t.contextDependency()).as("contextDependency for %s", t.name()).isBetween(0.0, 1.0);
            assertThat(t.emotionalTone()).as("emotionalTone for %s", t.name()).isBetween(0.0, 1.0);
        }
    }

    @Test
    void all_terms_have_non_empty_description() {
        for (Sarc7Term t : Sarc7Term.values()) {
            assertThat(t.description())
                .as("description for %s", t.name())
                .isNotBlank();
        }
    }

    @Test
    void axis_unaware_exactMatch_returns_empty() {
        for (Sarc7Term t : Sarc7Term.values()) {
            assertThat(t.exactMatch(ConscientiousnessTerm.class)).isEmpty();
        }
    }
}

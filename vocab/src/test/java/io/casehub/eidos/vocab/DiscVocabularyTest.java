package io.casehub.eidos.vocab;

import io.casehub.eidos.api.DispositionAxis;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class DiscVocabularyTest {

    @Test
    void uri_constant_is_correct() {
        assertThat(DiscTerm.URI).isEqualTo("urn:casehub:vocab:disc");
    }

    @Test
    void has_four_terms() {
        assertThat(DiscTerm.values()).hasSize(4);
    }

    @Test
    void conscientiousness_disc_key_avoids_collision() {
        assertThat(DiscTerm.CONSCIENTIOUSNESS_DISC.value()).isEqualTo("conscientiousness-disc");
        assertThat(DiscTerm.CONSCIENTIOUSNESS_DISC.label()).isEqualTo("Analytical (DISC-C)");
    }

    @Test
    void aliases_present() {
        assertThat(DiscTerm.DOMINANCE.aliases()).containsExactly("D");
        assertThat(DiscTerm.INFLUENCE.aliases()).containsExactly("i");
        assertThat(DiscTerm.STEADINESS.aliases()).containsExactly("S");
        assertThat(DiscTerm.CONSCIENTIOUSNESS_DISC.aliases()).containsExactly("C");
    }

    // ── Conscientiousness axis mappings ──────────────────────────────────────

    @Test
    void dominance_maps_to_conscientiousness_on_each_axis() {
        assertThat(DiscTerm.DOMINANCE.axisExactMatch(ConscientiousnessTerm.class, DispositionAxis.SOCIAL_ORIENTATION))
            .hasValue(ConscientiousnessTerm.INDEPENDENT);
        assertThat(DiscTerm.DOMINANCE.axisExactMatch(ConscientiousnessTerm.class, DispositionAxis.RULE_FOLLOWING))
            .hasValue(ConscientiousnessTerm.FLEXIBLE);
        assertThat(DiscTerm.DOMINANCE.axisExactMatch(ConscientiousnessTerm.class, DispositionAxis.RISK_APPETITE))
            .hasValue(ConscientiousnessTerm.BOLD);
        assertThat(DiscTerm.DOMINANCE.axisExactMatch(ConscientiousnessTerm.class, DispositionAxis.AUTONOMY))
            .hasValue(ConscientiousnessTerm.AUTONOMOUS);
    }

    @Test
    void influence_maps_to_conscientiousness_on_each_axis() {
        assertThat(DiscTerm.INFLUENCE.axisExactMatch(ConscientiousnessTerm.class, DispositionAxis.SOCIAL_ORIENTATION))
            .hasValue(ConscientiousnessTerm.COLLABORATIVE);
        assertThat(DiscTerm.INFLUENCE.axisExactMatch(ConscientiousnessTerm.class, DispositionAxis.RULE_FOLLOWING))
            .hasValue(ConscientiousnessTerm.FLEXIBLE);
        assertThat(DiscTerm.INFLUENCE.axisExactMatch(ConscientiousnessTerm.class, DispositionAxis.RISK_APPETITE))
            .hasValue(ConscientiousnessTerm.MEASURED);
        assertThat(DiscTerm.INFLUENCE.axisExactMatch(ConscientiousnessTerm.class, DispositionAxis.AUTONOMY))
            .hasValue(ConscientiousnessTerm.SEMI_AUTONOMOUS);
    }

    @Test
    void steadiness_maps_to_conscientiousness_on_each_axis() {
        assertThat(DiscTerm.STEADINESS.axisExactMatch(ConscientiousnessTerm.class, DispositionAxis.SOCIAL_ORIENTATION))
            .hasValue(ConscientiousnessTerm.FACILITATIVE);
        assertThat(DiscTerm.STEADINESS.axisExactMatch(ConscientiousnessTerm.class, DispositionAxis.RULE_FOLLOWING))
            .hasValue(ConscientiousnessTerm.PRINCIPLED);
        assertThat(DiscTerm.STEADINESS.axisExactMatch(ConscientiousnessTerm.class, DispositionAxis.RISK_APPETITE))
            .hasValue(ConscientiousnessTerm.CONSERVATIVE);
        assertThat(DiscTerm.STEADINESS.axisExactMatch(ConscientiousnessTerm.class, DispositionAxis.AUTONOMY))
            .hasValue(ConscientiousnessTerm.DIRECTED);
    }

    @Test
    void conscientiousness_disc_maps_to_conscientiousness_on_each_axis() {
        assertThat(DiscTerm.CONSCIENTIOUSNESS_DISC.axisExactMatch(ConscientiousnessTerm.class, DispositionAxis.SOCIAL_ORIENTATION))
            .hasValue(ConscientiousnessTerm.INDEPENDENT);
        assertThat(DiscTerm.CONSCIENTIOUSNESS_DISC.axisExactMatch(ConscientiousnessTerm.class, DispositionAxis.RULE_FOLLOWING))
            .hasValue(ConscientiousnessTerm.STRICT);
        assertThat(DiscTerm.CONSCIENTIOUSNESS_DISC.axisExactMatch(ConscientiousnessTerm.class, DispositionAxis.RISK_APPETITE))
            .hasValue(ConscientiousnessTerm.CONSERVATIVE);
        assertThat(DiscTerm.CONSCIENTIOUSNESS_DISC.axisExactMatch(ConscientiousnessTerm.class, DispositionAxis.AUTONOMY))
            .hasValue(ConscientiousnessTerm.SEMI_AUTONOMOUS);
    }

    @Test
    void disc_conflict_mode_axis_returns_empty_for_conscientiousness_target() {
        for (DiscTerm t : DiscTerm.values()) {
            assertThat(t.axisExactMatch(ConscientiousnessTerm.class, DispositionAxis.CONFLICT_MODE)).isEmpty();
        }
    }

    // ── ThomasKilmann conflict mode mappings ─────────────────────────────────

    @Test
    void dominance_maps_to_competing_on_conflict_mode() {
        assertThat(DiscTerm.DOMINANCE.axisExactMatch(ThomasKilmannTerm.class, DispositionAxis.CONFLICT_MODE))
            .hasValue(ThomasKilmannTerm.COMPETING);
    }

    @Test
    void influence_maps_to_collaborating_on_conflict_mode() {
        assertThat(DiscTerm.INFLUENCE.axisExactMatch(ThomasKilmannTerm.class, DispositionAxis.CONFLICT_MODE))
            .hasValue(ThomasKilmannTerm.COLLABORATING);
    }

    @Test
    void steadiness_maps_to_accommodating_on_conflict_mode() {
        assertThat(DiscTerm.STEADINESS.axisExactMatch(ThomasKilmannTerm.class, DispositionAxis.CONFLICT_MODE))
            .hasValue(ThomasKilmannTerm.ACCOMMODATING);
    }

    @Test
    void conscientiousness_disc_maps_to_avoiding_on_conflict_mode() {
        assertThat(DiscTerm.CONSCIENTIOUSNESS_DISC.axisExactMatch(ThomasKilmannTerm.class, DispositionAxis.CONFLICT_MODE))
            .hasValue(ThomasKilmannTerm.AVOIDING);
    }

    @Test
    void disc_non_conflict_axes_return_empty_for_tk_target() {
        for (DiscTerm t : DiscTerm.values()) {
            assertThat(t.axisExactMatch(ThomasKilmannTerm.class, DispositionAxis.SOCIAL_ORIENTATION)).isEmpty();
            assertThat(t.axisExactMatch(ThomasKilmannTerm.class, DispositionAxis.RULE_FOLLOWING)).isEmpty();
            assertThat(t.axisExactMatch(ThomasKilmannTerm.class, DispositionAxis.RISK_APPETITE)).isEmpty();
            assertThat(t.axisExactMatch(ThomasKilmannTerm.class, DispositionAxis.AUTONOMY)).isEmpty();
        }
    }

    @Test
    void unknown_target_vocab_returns_empty_for_all_terms_and_all_axes() {
        for (DiscTerm t : DiscTerm.values()) {
            for (DispositionAxis axis : DispositionAxis.values()) {
                assertThat(t.axisExactMatch(SvoTerm.class, axis)).isEmpty();
            }
        }
    }

    @Test
    void axis_unaware_exactMatch_returns_empty() {
        for (DiscTerm t : DiscTerm.values()) {
            assertThat(t.exactMatch(ConscientiousnessTerm.class)).isEmpty();
        }
    }

    @Test
    void conscientiousness_terms_remain_resolvable_after_disc_registration() {
        assertThat(ConscientiousnessTerm.COLLABORATIVE.value()).isEqualTo("collaborative");
        assertThat(DiscTerm.INFLUENCE.value()).isEqualTo("influence");
        assertThat(ThomasKilmannTerm.COLLABORATING.value()).isEqualTo("collaborating");
    }
}

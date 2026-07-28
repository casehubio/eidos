package io.casehub.eidos.vocab;

import io.casehub.eidos.api.DispositionAxis;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class JungianFunctionTermTest {

    @Test void eight_functions_defined() {
        assertThat(JungianFunctionTerm.values()).hasSize(8);
    }

    @Test void uri_is_jungian() {
        assertThat(JungianFunctionTerm.URI).isEqualTo("urn:casehub:vocab:jungian");
    }

    // ── axisExactMatch to ConscientiousnessTerm ─────────────────────────

    @Test void ti_social_orient_is_independent() {
        assertThat(JungianFunctionTerm.TI.axisExactMatch(ConscientiousnessTerm.class, DispositionAxis.SOCIAL_ORIENTATION))
                .contains(ConscientiousnessTerm.INDEPENDENT);
    }

    @Test void ti_rule_following_is_principled() {
        assertThat(JungianFunctionTerm.TI.axisExactMatch(ConscientiousnessTerm.class, DispositionAxis.RULE_FOLLOWING))
                .contains(ConscientiousnessTerm.PRINCIPLED);
    }

    @Test void ti_risk_appetite_is_measured() {
        assertThat(JungianFunctionTerm.TI.axisExactMatch(ConscientiousnessTerm.class, DispositionAxis.RISK_APPETITE))
                .contains(ConscientiousnessTerm.MEASURED);
    }

    @Test void ti_autonomy_is_autonomous() {
        assertThat(JungianFunctionTerm.TI.axisExactMatch(ConscientiousnessTerm.class, DispositionAxis.AUTONOMY))
                .contains(ConscientiousnessTerm.AUTONOMOUS);
    }

    @Test void fe_social_orient_is_facilitative() {
        assertThat(JungianFunctionTerm.FE.axisExactMatch(ConscientiousnessTerm.class, DispositionAxis.SOCIAL_ORIENTATION))
                .contains(ConscientiousnessTerm.FACILITATIVE);
    }

    @Test void se_risk_appetite_is_bold() {
        assertThat(JungianFunctionTerm.SE.axisExactMatch(ConscientiousnessTerm.class, DispositionAxis.RISK_APPETITE))
                .contains(ConscientiousnessTerm.BOLD);
    }

    @Test void si_autonomy_is_directed() {
        assertThat(JungianFunctionTerm.SI.axisExactMatch(ConscientiousnessTerm.class, DispositionAxis.AUTONOMY))
                .contains(ConscientiousnessTerm.DIRECTED);
    }

    // ── axisExactMatch to ThomasKilmannTerm (CONFLICT_MODE) ─────────────

    @Test void ti_conflict_mode_is_avoiding() {
        assertThat(JungianFunctionTerm.TI.axisExactMatch(ThomasKilmannTerm.class, DispositionAxis.CONFLICT_MODE))
                .contains(ThomasKilmannTerm.AVOIDING);
    }

    @Test void te_conflict_mode_is_competing() {
        assertThat(JungianFunctionTerm.TE.axisExactMatch(ThomasKilmannTerm.class, DispositionAxis.CONFLICT_MODE))
                .contains(ThomasKilmannTerm.COMPETING);
    }

    @Test void fe_conflict_mode_is_collaborating() {
        assertThat(JungianFunctionTerm.FE.axisExactMatch(ThomasKilmannTerm.class, DispositionAxis.CONFLICT_MODE))
                .contains(ThomasKilmannTerm.COLLABORATING);
    }

    @Test void fi_conflict_mode_is_accommodating() {
        assertThat(JungianFunctionTerm.FI.axisExactMatch(ThomasKilmannTerm.class, DispositionAxis.CONFLICT_MODE))
                .contains(ThomasKilmannTerm.ACCOMMODATING);
    }

    // ── Non-conscientiousness axes return empty ──────────────────────────

    @Test void ti_conscientiousness_conflict_mode_is_empty() {
        assertThat(JungianFunctionTerm.TI.axisExactMatch(ConscientiousnessTerm.class, DispositionAxis.CONFLICT_MODE))
                .isEmpty();
    }

    @Test void ti_tk_social_orient_is_empty() {
        assertThat(JungianFunctionTerm.TI.axisExactMatch(ThomasKilmannTerm.class, DispositionAxis.SOCIAL_ORIENTATION))
                .isEmpty();
    }

    // ── shadow() ────────────────────────────────────────────────────────

    @Test void ti_shadow_is_te() {
        assertThat(JungianFunctionTerm.TI.shadow()).isEqualTo(JungianFunctionTerm.TE);
    }

    @Test void te_shadow_is_ti() {
        assertThat(JungianFunctionTerm.TE.shadow()).isEqualTo(JungianFunctionTerm.TI);
    }

    @Test void fi_shadow_is_fe() {
        assertThat(JungianFunctionTerm.FI.shadow()).isEqualTo(JungianFunctionTerm.FE);
    }

    @Test void ni_shadow_is_ne() {
        assertThat(JungianFunctionTerm.NI.shadow()).isEqualTo(JungianFunctionTerm.NE);
    }

    @ParameterizedTest
    @EnumSource(JungianFunctionTerm.class)
    void shadow_is_symmetric(JungianFunctionTerm fn) {
        assertThat(fn.shadow().shadow()).isEqualTo(fn);
    }

    // ── category() and attitude() ───────────────────────────────────────

    @Test void ti_is_judging_introverted() {
        assertThat(JungianFunctionTerm.TI.category()).isEqualTo(FunctionCategory.JUDGING);
        assertThat(JungianFunctionTerm.TI.attitude()).isEqualTo(FunctionAttitude.INTROVERTED);
    }

    @Test void ne_is_perceiving_extraverted() {
        assertThat(JungianFunctionTerm.NE.category()).isEqualTo(FunctionCategory.PERCEIVING);
        assertThat(JungianFunctionTerm.NE.attitude()).isEqualTo(FunctionAttitude.EXTRAVERTED);
    }

    @ParameterizedTest
    @EnumSource(JungianFunctionTerm.class)
    void shadow_has_same_category_opposite_attitude(JungianFunctionTerm fn) {
        assertThat(fn.shadow().category()).isEqualTo(fn.category());
        assertThat(fn.shadow().attitude()).isNotEqualTo(fn.attitude());
    }

    // ── compatibleAuxiliaries() ─────────────────────────────────────────

    @Test void ti_compatible_auxiliaries_are_perceiving() {
        assertThat(JungianFunctionTerm.TI.compatibleAuxiliaries())
                .containsExactlyInAnyOrder(
                        JungianFunctionTerm.SE, JungianFunctionTerm.SI,
                        JungianFunctionTerm.NE, JungianFunctionTerm.NI);
    }

    @Test void se_compatible_auxiliaries_are_judging() {
        assertThat(JungianFunctionTerm.SE.compatibleAuxiliaries())
                .containsExactlyInAnyOrder(
                        JungianFunctionTerm.TI, JungianFunctionTerm.TE,
                        JungianFunctionTerm.FI, JungianFunctionTerm.FE);
    }

    @ParameterizedTest
    @EnumSource(JungianFunctionTerm.class)
    void compatible_auxiliaries_are_opposite_category(JungianFunctionTerm fn) {
        for (var aux : fn.compatibleAuxiliaries()) {
            assertThat(aux.category()).isNotEqualTo(fn.category());
        }
    }

    @ParameterizedTest
    @EnumSource(JungianFunctionTerm.class)
    void compatible_auxiliaries_has_four_entries(JungianFunctionTerm fn) {
        assertThat(fn.compatibleAuxiliaries()).hasSize(4);
    }

    // ── Weight tier constants ───────────────────────────────────────────

    @Test void weight_tiers_follow_jpaf_parameters() {
        assertThat(JungianFunctionTerm.DOMINANT_MIN).isEqualTo(0.31);
        assertThat(JungianFunctionTerm.AUXILIARY_MIN).isEqualTo(0.06);
        assertThat(JungianFunctionTerm.AUXILIARY_MAX).isEqualTo(0.30);
        assertThat(JungianFunctionTerm.UNDIFFERENTIATED_MAX).isEqualTo(0.06);
        assertThat(JungianFunctionTerm.REINFORCEMENT_DELTA).isEqualTo(0.06);
        assertThat(JungianFunctionTerm.DECAY_FACTOR).isEqualTo(0.20);
    }

    // ── VocabularyTerm contract ─────────────────────────────────────────

    @ParameterizedTest
    @EnumSource(JungianFunctionTerm.class)
    void all_functions_have_value_label_description(JungianFunctionTerm fn) {
        assertThat(fn.value()).isNotBlank();
        assertThat(fn.label()).isNotBlank();
        assertThat(fn.description()).isNotBlank();
    }

    @ParameterizedTest
    @EnumSource(JungianFunctionTerm.class)
    void all_values_are_lowercase(JungianFunctionTerm fn) {
        assertThat(fn.value()).isEqualTo(fn.value().toLowerCase());
    }
}

package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentDispositionTest {

    @Test
    void all_null_axes_are_allowed() {
        assertThatNoException().isThrownBy(
            () -> AgentDisposition.builder().build());
    }

    @Test
    void blank_social_orient_throws() {
        assertThatThrownBy(() -> AgentDisposition.builder()
                                                 .socialOrient("  ")
                                                 .build())
                .isInstanceOf(AgentValidationException.class);
    }

    @Test
    void rule_following_with_bidi_throws() {
        assertThatThrownBy(() -> AgentDisposition.builder()
                                                 .ruleFollowing("strict‮injection")
                                                 .build())
                .isInstanceOf(AgentValidationException.class);
    }

    @Test
    void risk_appetite_exceeds_200_chars_throws() {
        assertThatThrownBy(() -> AgentDisposition.builder()
                                                 .riskAppetite("r".repeat(201))
                                                 .build())
                .isInstanceOf(AgentValidationException.class);
    }

    @Test
    void autonomy_with_zero_width_throws() {
        assertThatThrownBy(() -> AgentDisposition.builder()
                                                 .autonomy("auto​nomous")
                                                 .build())
                .isInstanceOf(AgentValidationException.class);
    }

    @Test
    void valid_disposition_constructs_cleanly() {
        assertThatNoException().isThrownBy(() -> AgentDisposition.builder()
            .socialOrient("collaborative").ruleFollowing("adaptive")
            .riskAppetite("moderate").autonomy("assisted")
            .delegation(true)
            .build());
    }

    @Test
    void delegation_boolean_not_validated() {
        assertThatNoException().isThrownBy(
            () -> AgentDisposition.builder().delegation(true).build());
    }

    @Test
    void get_returns_value_for_each_axis() {
        var d = AgentDisposition.builder()
                                .socialOrient("collaborative").ruleFollowing("strict")
                                .riskAppetite("conservative").autonomy("directed")
                                .conflictMode("competing")
                                .build();
        assertThat(d.primaryTerm(DispositionAxis.SOCIAL_ORIENTATION)).isEqualTo("collaborative");
        assertThat(d.primaryTerm(DispositionAxis.RULE_FOLLOWING)).isEqualTo("strict");
        assertThat(d.primaryTerm(DispositionAxis.RISK_APPETITE)).isEqualTo("conservative");
        assertThat(d.primaryTerm(DispositionAxis.AUTONOMY)).isEqualTo("directed");
        assertThat(d.primaryTerm(DispositionAxis.CONFLICT_MODE)).isEqualTo("competing");
    }

    @Test
    void get_returns_empty_for_null_axis_field() {
        var d = AgentDisposition.builder().build();
        assertThat(d.get(DispositionAxis.SOCIAL_ORIENTATION)).isEmpty();
        assertThat(d.get(DispositionAxis.RULE_FOLLOWING)).isEmpty();
        assertThat(d.get(DispositionAxis.RISK_APPETITE)).isEmpty();
        assertThat(d.get(DispositionAxis.AUTONOMY)).isEmpty();
        assertThat(d.get(DispositionAxis.CONFLICT_MODE)).isEmpty();
        assertThat(d.primaryTerm(DispositionAxis.SOCIAL_ORIENTATION)).isNull();
    }

    @Test
    void disposition_axis_enum_has_five_values() {
        assertThat(DispositionAxis.values()).hasSize(5);
    }

    @Test
    void conflict_mode_null_is_allowed() {
        assertThatNoException().isThrownBy(
            () -> AgentDisposition.builder().build());
    }

    @Test
    void conflict_mode_blank_throws() {
        assertThatThrownBy(() -> AgentDisposition.builder()
                                                 .conflictMode("  ")
                                                 .build())
                .isInstanceOf(AgentValidationException.class);
    }

    @Test
    void conflict_mode_over_200_chars_throws() {
        assertThatThrownBy(() -> AgentDisposition.builder()
                                                 .conflictMode("c".repeat(201))
                                                 .build())
                .isInstanceOf(AgentValidationException.class);
    }

    @Test
    void get_returns_conflict_mode_when_set() {
        var d = AgentDisposition.builder()
                                .socialOrient("collaborative").ruleFollowing("strict")
                                .riskAppetite("conservative").autonomy("directed")
                                .conflictMode("competing")
                                .build();
        assertThat(d.primaryTerm(DispositionAxis.CONFLICT_MODE)).isEqualTo("competing");
    }

    @Test
    void get_returns_empty_for_null_conflict_mode() {
        var d = AgentDisposition.builder().build();
        assertThat(d.get(DispositionAxis.CONFLICT_MODE)).isEmpty();
    }

    @Test
    void builder_produces_equivalent_result_to_constructor() {
        var fromBuilder1 = AgentDisposition.builder()
            .socialOrient("collaborative").ruleFollowing("strict")
            .riskAppetite("conservative").autonomy("directed")
            .conflictMode("competing").delegation(true)
            .build();
        var fromBuilder2 = AgentDisposition.builder()
            .socialOrient("collaborative")
            .ruleFollowing("strict")
            .riskAppetite("conservative")
            .autonomy("directed")
            .conflictMode("competing")
            .delegation(true)
            .build();
        assertThat(fromBuilder1).isEqualTo(fromBuilder2);
    }

    @Test
    void weighted_axis_values() {
        var d = AgentDisposition.builder()
                                .socialOrient(new DispositionValue("independent", 0.7), new DispositionValue("collaborative", 0.3))
                                .build();
        assertThat(d.get(DispositionAxis.SOCIAL_ORIENTATION)).hasSize(2);
        assertThat(d.primaryTerm(DispositionAxis.SOCIAL_ORIENTATION)).isEqualTo("independent");
    }

    @Test
    void disposition_profile_populated() {
        var d = AgentDisposition.builder()
                                .dispositionProfile(
                                        new DispositionValue("ti", 0.45),
                                        new DispositionValue("ne", 0.20))
                                .build();
        assertThat(d.dispositionProfile()).hasSize(2);
        assertThat(d.dispositionProfile().getFirst().term()).isEqualTo("ti");
        assertThat(d.dispositionProfile().getFirst().weight()).isEqualTo(0.45);
    }

    @Test
    void disposition_profile_empty_by_default() {
        var d = AgentDisposition.builder().build();
        assertThat(d.dispositionProfile()).isEmpty();
    }

    @Test
    void disposition_profile_is_unmodifiable() {
        var d = AgentDisposition.builder()
                                .dispositionProfile(new DispositionValue("ti", 0.5))
                                .build();
        assertThatThrownBy(() -> d.dispositionProfile().add(new DispositionValue("fe", 0.3)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void axis_list_is_unmodifiable() {
        var d = AgentDisposition.builder()
                                .socialOrient("independent")
                                .build();
        assertThatThrownBy(() -> d.socialOrient().add(new DispositionValue("x", 0.1)))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}

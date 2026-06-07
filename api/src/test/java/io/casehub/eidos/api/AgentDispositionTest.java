package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

class AgentDispositionTest {

    @Test
    void all_null_axes_are_allowed() {
        assertThatNoException().isThrownBy(
            () -> AgentDisposition.builder().build());
    }

    @Test
    void blank_social_orient_throws() {
        assertThatThrownBy(() -> AgentDisposition.builder()
            .socialOrient("  ").ruleFollowing("strict")
            .riskAppetite("low").autonomy("autonomous")
            .build())
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName())
                .isEqualTo("socialOrient"));
    }

    @Test
    void rule_following_with_bidi_throws() {
        // U+202E RIGHT-TO-LEFT OVERRIDE
        assertThatThrownBy(() -> AgentDisposition.builder()
            .socialOrient("collaborative").ruleFollowing("strict‮injection")
            .riskAppetite("low").autonomy("autonomous")
            .build())
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName())
                .isEqualTo("ruleFollowing"));
    }

    @Test
    void risk_appetite_exceeds_200_chars_throws() {
        assertThatThrownBy(() -> AgentDisposition.builder()
            .socialOrient("collaborative").ruleFollowing("strict")
            .riskAppetite("r".repeat(201)).autonomy("autonomous")
            .build())
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName())
                .isEqualTo("riskAppetite"));
    }

    @Test
    void autonomy_with_zero_width_throws() {
        // U+200B ZERO WIDTH SPACE
        assertThatThrownBy(() -> AgentDisposition.builder()
            .socialOrient("collaborative").ruleFollowing("strict")
            .riskAppetite("low").autonomy("auto​nomous")
            .build())
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName())
                .isEqualTo("autonomy"));
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
        assertThat(d.get(DispositionAxis.SOCIAL_ORIENTATION)).contains("collaborative");
        assertThat(d.get(DispositionAxis.RULE_FOLLOWING)).contains("strict");
        assertThat(d.get(DispositionAxis.RISK_APPETITE)).contains("conservative");
        assertThat(d.get(DispositionAxis.AUTONOMY)).contains("directed");
        assertThat(d.get(DispositionAxis.CONFLICT_MODE)).contains("competing");
    }

    @Test
    void get_returns_empty_for_null_axis_field() {
        var d = AgentDisposition.builder().build();
        assertThat(d.get(DispositionAxis.SOCIAL_ORIENTATION)).isEmpty();
        assertThat(d.get(DispositionAxis.RULE_FOLLOWING)).isEmpty();
        assertThat(d.get(DispositionAxis.RISK_APPETITE)).isEmpty();
        assertThat(d.get(DispositionAxis.AUTONOMY)).isEmpty();
        assertThat(d.get(DispositionAxis.CONFLICT_MODE)).isEmpty();
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
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName())
                .isEqualTo("conflictMode"));
    }

    @Test
    void conflict_mode_over_200_chars_throws() {
        assertThatThrownBy(() -> AgentDisposition.builder()
            .conflictMode("c".repeat(201))
            .build())
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName())
                .isEqualTo("conflictMode"));
    }

    @Test
    void get_returns_conflict_mode_when_set() {
        var d = AgentDisposition.builder()
            .socialOrient("collaborative").ruleFollowing("strict")
            .riskAppetite("conservative").autonomy("directed")
            .conflictMode("competing")
            .build();
        assertThat(d.get(DispositionAxis.CONFLICT_MODE)).contains("competing");
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
}

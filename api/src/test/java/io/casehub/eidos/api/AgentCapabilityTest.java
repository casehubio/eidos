package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

class AgentCapabilityTest {

    static AgentCapability valid() {
        return new AgentCapability("code-review", 0.9, 100L, "low",
            List.of("pull-request"), List.of("review-comment"), List.of("java"),
            Map.of("java", 0.95));
    }

    // ── name (required) ────────────────────────────────────────────────────────

    @Test
    void name_null_throws() {
        assertThatThrownBy(() ->
            new AgentCapability(null, null, null, null, List.of(), List.of(), List.of(), Map.of()))
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName())
                .isEqualTo("capability.name"));
    }

    @Test
    void name_blank_throws() {
        assertThatThrownBy(() ->
            new AgentCapability("  ", null, null, null, List.of(), List.of(), List.of(), Map.of()))
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName()).isEqualTo("capability.name"));
    }

    @Test
    void name_exceeds_100_chars_throws() {
        assertThatThrownBy(() ->
            new AgentCapability("n".repeat(101), null, null, null, List.of(), List.of(), List.of(), Map.of()))
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName()).isEqualTo("capability.name"));
    }

    @Test
    void name_with_bidi_control_throws() {
        // U+202E RIGHT-TO-LEFT OVERRIDE — a banned BiDi character
        assertThatThrownBy(() ->
            new AgentCapability("code‮review", null, null, null, List.of(), List.of(), List.of(), Map.of()))
            .isInstanceOf(AgentValidationException.class);
    }

    @Test
    void name_at_exactly_100_chars_is_valid() {
        assertThatNoException().isThrownBy(() ->
            new AgentCapability("n".repeat(100), null, null, null, List.of(), List.of(), List.of(), Map.of()));
    }

    // ── costHint (optional) ────────────────────────────────────────────────────

    @Test
    void cost_hint_null_is_allowed() {
        assertThatNoException().isThrownBy(() ->
            new AgentCapability("review", null, null, null, List.of(), List.of(), List.of(), Map.of()));
    }

    @Test
    void cost_hint_blank_throws() {
        assertThatThrownBy(() ->
            new AgentCapability("review", null, null, "  ", List.of(), List.of(), List.of(), Map.of()))
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName())
                .isEqualTo("costHint"));
    }

    @Test
    void cost_hint_with_injection_char_throws() {
        // U+200B ZERO WIDTH SPACE
        assertThatThrownBy(() ->
            new AgentCapability("review", null, null, "low​cost", List.of(), List.of(), List.of(), Map.of()))
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName()).isEqualTo("costHint"));
    }

    // ── list fields ────────────────────────────────────────────────────────────

    @Test
    void input_types_null_list_is_allowed() {
        assertThatNoException().isThrownBy(() ->
            new AgentCapability("review", null, null, null, null, List.of(), List.of(), Map.of()));
    }

    @Test
    void input_types_blank_item_throws_with_index() {
        assertThatThrownBy(() ->
            new AgentCapability("review", null, null, null,
                List.of("pr", ""), List.of(), List.of(), Map.of()))
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName())
                .isEqualTo("inputTypes[1]"));
    }

    @Test
    void output_types_item_with_c0_control_char_throws() {
        // U+0001 START OF HEADING — a C0 control character
        assertThatThrownBy(() ->
            new AgentCapability("review", null, null, null,
                List.of(), List.of("commentinject"), List.of(), Map.of()))
            .isInstanceOf(AgentValidationException.class);
    }

    @Test
    void tags_item_exceeds_200_chars_throws() {
        assertThatThrownBy(() ->
            new AgentCapability("review", null, null, null,
                List.of(), List.of(), List.of("t".repeat(201)), Map.of()))
            .isInstanceOf(AgentValidationException.class);
    }

    // ── epistemicDomains keys ──────────────────────────────────────────────────

    @Test
    void epistemic_domain_key_with_alm_throws() {
        // U+061C ARABIC LETTER MARK — a banned BiDi control character
        assertThatThrownBy(() ->
            new AgentCapability("review", null, null, null,
                List.of(), List.of(), List.of(), Map.of("java؜domain", 0.9)))
            .isInstanceOf(AgentValidationException.class);
    }

    @Test
    void epistemic_domain_null_map_is_allowed() {
        assertThatNoException().isThrownBy(() ->
            new AgentCapability("review", null, null, null, List.of(), List.of(), List.of(), null));
    }

    // ── full valid construction ────────────────────────────────────────────────

    @Test
    void valid_capability_constructs_cleanly() {
        assertThatNoException().isThrownBy(AgentCapabilityTest::valid);
    }
}

package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

class AgentCapabilityTest {

    static AgentCapability valid() {
        return AgentCapability.builder()
            .name("code-review").qualityHint(0.9).latencyHintP50Ms(100L).costHint("low")
            .inputTypes(List.of("pull-request")).outputTypes(List.of("review-comment"))
            .tags(List.of("java")).epistemicDomains(Map.of("java", 0.95))
            .build();
    }

    // ── name (required) ────────────────────────────────────────────────────────

    @Test
    void name_null_throws() {
        assertThatThrownBy(() ->
            AgentCapability.builder().name(null).build())
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName())
                .isEqualTo("capability.name"));
    }

    @Test
    void name_blank_throws() {
        assertThatThrownBy(() ->
            AgentCapability.builder().name("  ").build())
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName()).isEqualTo("capability.name"));
    }

    @Test
    void name_exceeds_100_chars_throws() {
        assertThatThrownBy(() ->
            AgentCapability.builder().name("n".repeat(101)).build())
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName()).isEqualTo("capability.name"));
    }

    @Test
    void name_with_bidi_control_throws() {
        // U+202E RIGHT-TO-LEFT OVERRIDE — a banned BiDi character
        assertThatThrownBy(() ->
            AgentCapability.builder().name("code‮review").build())
            .isInstanceOf(AgentValidationException.class);
    }

    @Test
    void name_at_exactly_100_chars_is_valid() {
        assertThatNoException().isThrownBy(() ->
            AgentCapability.builder().name("n".repeat(100)).build());
    }

    // ── costHint (optional) ────────────────────────────────────────────────────

    @Test
    void cost_hint_null_is_allowed() {
        assertThatNoException().isThrownBy(() ->
            AgentCapability.builder().name("review").build());
    }

    @Test
    void cost_hint_blank_throws() {
        assertThatThrownBy(() ->
            AgentCapability.builder().name("review").costHint("  ").build())
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName())
                .isEqualTo("costHint"));
    }

    @Test
    void cost_hint_with_injection_char_throws() {
        // U+200B ZERO WIDTH SPACE
        assertThatThrownBy(() ->
            AgentCapability.builder().name("review").costHint("low​cost").build())
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName()).isEqualTo("costHint"));
    }

    // ── list fields ────────────────────────────────────────────────────────────

    @Test
    void input_types_null_list_is_allowed() {
        assertThatNoException().isThrownBy(() ->
            AgentCapability.builder().name("review").inputTypes(null).build());
    }

    @Test
    void input_types_blank_item_throws_with_index() {
        assertThatThrownBy(() ->
            AgentCapability.builder().name("review").inputTypes(List.of("pr", "")).build())
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName())
                .isEqualTo("inputTypes[1]"));
    }

    @Test
    void output_types_item_with_c0_control_char_throws() {
        // U+0001 START OF HEADING — a C0 control character
        assertThatThrownBy(() ->
            AgentCapability.builder().name("review").outputTypes(List.of("commentinject")).build())
            .isInstanceOf(AgentValidationException.class);
    }

    @Test
    void tags_item_exceeds_200_chars_throws() {
        assertThatThrownBy(() ->
            AgentCapability.builder().name("review").tags(List.of("t".repeat(201))).build())
            .isInstanceOf(AgentValidationException.class);
    }

    // ── epistemicDomains keys ──────────────────────────────────────────────────

    @Test
    void epistemic_domain_key_with_alm_throws() {
        // U+061C ARABIC LETTER MARK — a banned BiDi control character
        assertThatThrownBy(() ->
            AgentCapability.builder().name("review").epistemicDomains(Map.of("java؜domain", 0.9)).build())
            .isInstanceOf(AgentValidationException.class);
    }

    @Test
    void epistemic_domain_null_map_is_allowed() {
        assertThatNoException().isThrownBy(() ->
            AgentCapability.builder().name("review").epistemicDomains(null).build());
    }

    // ── full valid construction ────────────────────────────────────────────────

    @Test
    void valid_capability_constructs_cleanly() {
        assertThatNoException().isThrownBy(AgentCapabilityTest::valid);
    }

    // ── excludedDomains ────────────────────────────────────────────────────────

    @Test
    void excluded_domains_null_is_allowed() {
        assertThatNoException().isThrownBy(() ->
            AgentCapability.builder().name("review").build());
    }

    @Test
    void excluded_domains_blank_entry_throws() {
        assertThatThrownBy(() ->
            AgentCapability.builder()
                .name("review")
                .excludedDomains(java.util.Set.of("rust", "  "))
                .build())
            .isInstanceOf(AgentValidationException.class);
    }

    @Test
    void excluded_domains_over_length_throws() {
        assertThatThrownBy(() ->
            AgentCapability.builder()
                .name("review")
                .excludedDomains(java.util.Set.of("d".repeat(201)))
                .build())
            .isInstanceOf(AgentValidationException.class);
    }

    @Test
    void excluded_domains_entry_with_bidi_control_throws() {
        // U+202E RIGHT-TO-LEFT OVERRIDE — a banned BiDi character
        assertThatThrownBy(() ->
            AgentCapability.builder()
                .name("review")
                .excludedDomains(java.util.Set.of("rust‮lang"))
                .build())
            .isInstanceOf(AgentValidationException.class);
    }

    @Test
    void excluded_domains_defensive_copy_prevents_external_mutation() {
        var domains = new java.util.HashSet<>(java.util.Set.of("rust"));
        var cap = AgentCapability.builder().name("review").excludedDomains(domains).build();
        domains.add("go");
        assertThat(cap.excludedDomains()).doesNotContain("go");
        assertThatThrownBy(() -> cap.excludedDomains().add("python"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void domain_in_both_excluded_and_epistemic_throws() {
        assertThatThrownBy(() ->
            AgentCapability.builder()
                .name("security-review")
                .epistemicDomains(Map.of("java", 0.95))
                .excludedDomains(java.util.Set.of("java"))
                .build())
            .isInstanceOf(AgentValidationException.class)
            .hasMessageContaining("java");
    }

    @Test
    void builder_round_trips_all_fields() {
        var cap = AgentCapability.builder()
            .name("code-review")
            .qualityHint(0.9)
            .latencyHintP50Ms(100L)
            .costHint("low")
            .inputTypes(List.of("pull-request"))
            .outputTypes(List.of("review-comment"))
            .tags(List.of("java"))
            .epistemicDomains(Map.of("java", 0.95))
            .excludedDomains(java.util.Set.of("rust"))
            .build();
        assertThat(cap.name()).isEqualTo("code-review");
        assertThat(cap.qualityHint()).isEqualTo(0.9);
        assertThat(cap.latencyHintP50Ms()).isEqualTo(100L);
        assertThat(cap.costHint()).isEqualTo("low");
        assertThat(cap.inputTypes()).containsExactly("pull-request");
        assertThat(cap.outputTypes()).containsExactly("review-comment");
        assertThat(cap.tags()).containsExactly("java");
        assertThat(cap.epistemicDomains()).containsEntry("java", 0.95);
        assertThat(cap.excludedDomains()).containsExactly("rust");
    }

    // ── capabilityVocabulary (optional) ────────────────────────────────────────

    @Test
    void capability_vocabulary_carried_through_builder() {
        var cap = AgentCapability.builder()
            .name("code-review")
            .capabilityVocabulary("urn:casehub:vocab:capability")
            .build();
        assertThat(cap.capabilityVocabulary()).isEqualTo("urn:casehub:vocab:capability");
    }

    @Test
    void capability_vocabulary_null_is_valid() {
        var cap = AgentCapability.builder()
            .name("code-review")
            .build();
        assertThat(cap.capabilityVocabulary()).isNull();
    }
}

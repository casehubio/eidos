package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentDescriptorValidatorTest {

    static final String VALID_ID    = "agent-1";
    static final String VALID_NAME  = "My Agent";
    static final String VALID_SLOT  = "reviewer";
    static final String VALID_TID   = "tenant-1";

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidCases")
    void invalid_field_throws_with_field_name(final String label, final String agentId,
                                               final String name, final String slot,
                                               final String tenancyId,
                                               final String expectedField) {
        assertThatThrownBy(() -> AgentDescriptorValidator.validate(agentId, name, slot, tenancyId))
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName())
                .isEqualTo(expectedField));
    }

    static Stream<Arguments> invalidCases() {
        return Stream.of(
            // null checks
            Arguments.of("agentId null",    null,       VALID_NAME, VALID_SLOT, VALID_TID,  "agentId"),
            Arguments.of("name null",       VALID_ID,   null,       VALID_SLOT, VALID_TID,  "name"),
            Arguments.of("slot null",       VALID_ID,   VALID_NAME, null,       VALID_TID,  "slot"),
            Arguments.of("tenancyId null",  VALID_ID,   VALID_NAME, VALID_SLOT, null,       "tenancyId"),
            // blank checks
            Arguments.of("agentId blank",   "   ",      VALID_NAME, VALID_SLOT, VALID_TID,  "agentId"),
            Arguments.of("name blank",      VALID_ID,   "",         VALID_SLOT, VALID_TID,  "name"),
            // length checks
            Arguments.of("agentId too long",  "a".repeat(256), VALID_NAME,       VALID_SLOT,       VALID_TID,        "agentId"),
            Arguments.of("name too long",     VALID_ID,        "n".repeat(201),  VALID_SLOT,       VALID_TID,        "name"),
            Arguments.of("slot too long",     VALID_ID,        VALID_NAME,       "s".repeat(101),  VALID_TID,        "slot"),
            Arguments.of("tenancyId too long",VALID_ID,        VALID_NAME,       VALID_SLOT,       "t".repeat(256),  "tenancyId"),
            // C0 control chars
            Arguments.of("name C0 tab",       VALID_ID, "name\twith\ttab",   VALID_SLOT, VALID_TID, "name"),
            Arguments.of("slot newline",       VALID_ID, VALID_NAME,          "slot\nwith\nnl", VALID_TID, "slot"),
            // DEL (0x7F)
            Arguments.of("name DEL",          VALID_ID, "namehidden",  VALID_SLOT, VALID_TID, "name"),
            // C1 control chars (U+0085 is NEL)
            Arguments.of("name C1 NEL",       VALID_ID, "nameend",     VALID_SLOT, VALID_TID, "name"),
            // BiDi overrides
            Arguments.of("name RLM",          VALID_ID, "name‏right",   VALID_SLOT, VALID_TID, "name"),
            Arguments.of("slot LRE",          VALID_ID, VALID_NAME,          "slot‪embed", VALID_TID, "slot"),
            Arguments.of("name LRI",          VALID_ID, "name⁦isolate", VALID_SLOT, VALID_TID, "name"),
            // Zero-width chars
            Arguments.of("name ZWSP",         VALID_ID, "name​zero",    VALID_SLOT, VALID_TID, "name"),
            Arguments.of("name BOM",          VALID_ID, "name﻿bom",     VALID_SLOT, VALID_TID, "name"),
            // Line/paragraph separators
            Arguments.of("name LS U+2028",    VALID_ID, "name sep",     VALID_SLOT, VALID_TID, "name"),
            Arguments.of("name PS U+2029",    VALID_ID, "name sep",     VALID_SLOT, VALID_TID, "name"),
            // Arabic Letter Mark (BiDi control, Unicode 6.3)
            Arguments.of("name ALM U+061C",   VALID_ID, "name؜alm", VALID_SLOT, VALID_TID, "name")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validCases")
    void valid_fields_do_not_throw(final String label, final String agentId, final String name,
                                    final String slot, final String tenancyId) {
        assertThatNoException().isThrownBy(
            () -> AgentDescriptorValidator.validate(agentId, name, slot, tenancyId));
    }

    static Stream<Arguments> validCases() {
        return Stream.of(
            Arguments.of("all simple",    VALID_ID,          VALID_NAME,          VALID_SLOT,          VALID_TID),
            Arguments.of("max agentId",   "a".repeat(255),   VALID_NAME,          VALID_SLOT,          VALID_TID),
            Arguments.of("max name",      VALID_ID,          "n".repeat(200),     VALID_SLOT,          VALID_TID),
            Arguments.of("max slot",      VALID_ID,          VALID_NAME,          "s".repeat(100),     VALID_TID),
            Arguments.of("unicode ok",    VALID_ID,          "Agent 中文", VALID_SLOT,         VALID_TID),
            Arguments.of("hyphen-dash",   "my-agent-v2",     VALID_NAME,          VALID_SLOT,          VALID_TID)
        );
    }

    // ── validateRequired ──────────────────────────────────────────────────────

    @Test
    void validateRequired_null_throws() {
        assertThatThrownBy(
            () -> AgentDescriptorValidator.validateRequired("capability.name", null, 100))
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName())
                .isEqualTo("capability.name"));
    }

    // ── validateOptional ──────────────────────────────────────────────────────

    @Test
    void validateOptional_null_is_allowed() {
        assertThatNoException().isThrownBy(
            () -> AgentDescriptorValidator.validateOptional("field", null, 200));
    }

    @Test
    void validateOptional_blank_throws() {
        assertThatThrownBy(
            () -> AgentDescriptorValidator.validateOptional("provider", "   ", 200))
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName())
                .isEqualTo("provider"));
    }

    @Test
    void validateOptional_exceeds_length_throws() {
        assertThatThrownBy(
            () -> AgentDescriptorValidator.validateOptional("version", "v".repeat(201), 200))
            .isInstanceOf(AgentValidationException.class);
    }

    @Test
    void validateOptional_bidi_control_throws() {
        // U+202E RIGHT-TO-LEFT OVERRIDE — a banned BiDi control character
        assertThatThrownBy(
            () -> AgentDescriptorValidator.validateOptional("jurisdiction", "EU‮evil", 1000))
            .isInstanceOf(AgentValidationException.class);
    }

    @Test
    void validateOptional_valid_value_does_not_throw() {
        assertThatNoException().isThrownBy(
            () -> AgentDescriptorValidator.validateOptional("provider", "anthropic", 200));
    }

    // ── validateItems ──────────────────────────────────────────────────────────

    @Test
    void validateItems_null_list_is_allowed() {
        assertThatNoException().isThrownBy(
            () -> AgentDescriptorValidator.validateItems("inputTypes", null, 200));
    }

    @Test
    void validateItems_blank_item_throws_with_index() {
        assertThatThrownBy(
            () -> AgentDescriptorValidator.validateItems("inputTypes", List.of("ok", ""), 200))
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName())
                .isEqualTo("inputTypes[1]"));
    }

    @Test
    void validateItems_injection_item_throws() {
        // U+200B ZERO WIDTH SPACE — a banned zero-width character
        assertThatThrownBy(
            () -> AgentDescriptorValidator.validateItems("tags", List.of("valid", "bad​zero"), 200))
            .isInstanceOf(AgentValidationException.class);
    }

    // ── briefing field ────────────────────────────────────────────────────────

    @Test
    void briefing_accepts_null() {
        assertThatNoException().isThrownBy(() ->
            AgentDescriptor.builder()
                .agentId("a").name("n").slot("s").tenancyId("t")
                .briefing(null)
                .build());
    }

    @Test
    void briefing_accepts_500_chars() {
        assertThatNoException().isThrownBy(() ->
            AgentDescriptor.builder()
                .agentId("a").name("n").slot("s").tenancyId("t")
                .briefing("x".repeat(500))
                .build());
    }

    @Test
    void briefing_rejects_2001_chars() {
        assertThatThrownBy(() ->
            AgentDescriptor.builder()
                .agentId("a").name("n").slot("s").tenancyId("t")
                .briefing("x".repeat(2001))
                .build())
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName())
                .isEqualTo("briefing"));
    }

    @Test
    void briefing_accepts_typical_principles() {
        assertThatNoException().isThrownBy(() ->
            AgentDescriptor.builder()
                .agentId("a").name("n").slot("s").tenancyId("t")
                .briefing("Speed is a feature. Review latency is a cost. 90% elegant beats perfect.")
                .build());
    }

    @Test
    void briefing_rejects_blank() {
        assertThatThrownBy(() ->
            AgentDescriptor.builder()
                .agentId("a").name("n").slot("s").tenancyId("t")
                .briefing("   ")
                .build())
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName())
                .isEqualTo("briefing"));
    }

    @Test
    void briefing_at_1500_chars_is_valid() {
        final String briefing = "A".repeat(1500);
        assertThatNoException().isThrownBy(() ->
            AgentDescriptor.builder()
                .agentId(VALID_ID).name(VALID_NAME).slot(VALID_SLOT).tenancyId(VALID_TID)
                .briefing(briefing)
                .build());
    }

    @Test
    void briefing_accepts_exactly_2000_chars() {
        final String briefing = "A".repeat(2000);
        assertThatNoException().isThrownBy(() ->
            AgentDescriptor.builder()
                .agentId(VALID_ID).name(VALID_NAME).slot(VALID_SLOT).tenancyId(VALID_TID)
                .briefing(briefing)
                .build());
    }

    @Test
    void briefing_accepts_newlines() {
        assertThatNoException().isThrownBy(() ->
            AgentDescriptor.builder()
                .agentId(VALID_ID).name(VALID_NAME).slot(VALID_SLOT).tenancyId(VALID_TID)
                .briefing("First paragraph.\n\nSecond paragraph.\nThird line.")
                .build());
    }

    @Test
    void briefing_rejects_tab() {
        assertThatThrownBy(() ->
            AgentDescriptor.builder()
                .agentId(VALID_ID).name(VALID_NAME).slot(VALID_SLOT).tenancyId(VALID_TID)
                .briefing("text\twith\ttab")
                .build())
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName())
                .isEqualTo("briefing"));
    }

    @Test
    void briefing_rejects_null_byte() {
        assertThatThrownBy(() ->
            AgentDescriptor.builder()
                .agentId(VALID_ID).name(VALID_NAME).slot(VALID_SLOT).tenancyId(VALID_TID)
                .briefing("text null")
                .build())
            .isInstanceOf(AgentValidationException.class);
    }

    // ── validateMapKeys ────────────────────────────────────────────────────────

    @Test
    void validateMapKeys_null_set_is_allowed() {
        assertThatNoException().isThrownBy(
            () -> AgentDescriptorValidator.validateMapKeys("epistemicDomains", null, 200));
    }

    @Test
    void validateMapKeys_bidi_key_throws() {
        // U+061C ARABIC LETTER MARK — a banned BiDi control character
        assertThatThrownBy(
            () -> AgentDescriptorValidator.validateMapKeys("epistemicDomains",
                Set.of("java؜injection"), 200))
            .isInstanceOf(AgentValidationException.class);
    }

    @Test
    void goals_default_to_empty_list() {
        var d = AgentDescriptor.builder()
                               .agentId("a").name("n").slot("s").tenancyId("t").build();
        assertThat(d.goals()).isEmpty();
        assertThat(d.constraints()).isEmpty();
    }

    @Test
    void duplicate_goal_names_throws() {
        var goals = List.of(
                new AgentGoal("find-diamond", "Find it", GoalPriority.PRIMARY, Visibility.PUBLIC),
                new AgentGoal("find-diamond", "Find it again", GoalPriority.SECONDARY, Visibility.PUBLIC));
        assertThatThrownBy(() -> AgentDescriptor.builder()
                                                .agentId("a").name("n").slot("s").tenancyId("t").goals(goals).build())
                .isInstanceOf(AgentValidationException.class)
                .hasMessageContaining("find-diamond");
    }

    @Test
    void duplicate_constraint_names_throws() {
        var constraints = List.of(
                new AgentConstraint("no-violence", "No violence", Visibility.PUBLIC),
                new AgentConstraint("no-violence", "Avoid violence", Visibility.PUBLIC));
        assertThatThrownBy(() -> AgentDescriptor.builder()
                                                .agentId("a").name("n").slot("s").tenancyId("t").constraints(constraints).build())
                .isInstanceOf(AgentValidationException.class)
                .hasMessageContaining("no-violence");
    }

    @Test
    void goals_exceeding_max_throws() {
        var goals = java.util.stream.IntStream.rangeClosed(1, 11)
                                              .mapToObj(i -> new AgentGoal("g-" + i, "desc", GoalPriority.PRIMARY, Visibility.PUBLIC))
                                              .toList();
        assertThatThrownBy(() -> AgentDescriptor.builder()
                                                .agentId("a").name("n").slot("s").tenancyId("t").goals(goals).build())
                .isInstanceOf(AgentValidationException.class)
                .hasMessageContaining("goals");
    }

    @Test
    void constraints_exceeding_max_throws() {
        var constraints = java.util.stream.IntStream.rangeClosed(1, 11)
                                                    .mapToObj(i -> new AgentConstraint("c-" + i, "desc", Visibility.PUBLIC))
                                                    .toList();
        assertThatThrownBy(() -> AgentDescriptor.builder()
                                                .agentId("a").name("n").slot("s").tenancyId("t").constraints(constraints).build())
                .isInstanceOf(AgentValidationException.class)
                .hasMessageContaining("constraints");
    }

    @Test
    void publicGoals_filters_by_visibility() {
        var goals = List.of(
                new AgentGoal("public-goal", "Visible", GoalPriority.PRIMARY, Visibility.PUBLIC),
                new AgentGoal("private-goal", "Hidden", GoalPriority.SECONDARY, Visibility.PRIVATE));
        var d = AgentDescriptor.builder()
                               .agentId("a").name("n").slot("s").tenancyId("t").goals(goals).build();
        assertThat(d.publicGoals()).hasSize(1);
        assertThat(d.publicGoals().get(0).name()).isEqualTo("public-goal");
    }

    @Test
    void publicConstraints_filters_by_visibility() {
        var constraints = List.of(
                new AgentConstraint("public-c", "Visible", Visibility.PUBLIC),
                new AgentConstraint("private-c", "Hidden", Visibility.PRIVATE));
        var d = AgentDescriptor.builder()
                               .agentId("a").name("n").slot("s").tenancyId("t").constraints(constraints).build();
        assertThat(d.publicConstraints()).hasSize(1);
        assertThat(d.publicConstraints().get(0).name()).isEqualTo("public-c");
    }
}

package io.casehub.eidos.api;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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
            .isInstanceOf(AgentDescriptorValidationException.class)
            .satisfies(ex -> assertThat(((AgentDescriptorValidationException) ex).fieldName())
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
}

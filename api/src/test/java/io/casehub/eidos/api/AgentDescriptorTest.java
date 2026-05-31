package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class AgentDescriptorTest {

    static AgentDescriptor minimal(String agentId, String tenancyId) {
        return new AgentDescriptor(
            agentId, "name", "1.0", "provider",
            "modelFamily", "modelVersion", null,
            null, null, null,
            "slot", List.of(),
            new AgentDisposition("collaborative", "principled", "measured", "semi-autonomous", false),
            null, null, tenancyId
        );
    }

    @Test
    void all_fields_accessible() {
        var d = minimal("agent-1", "default");
        assertThat(d.agentId()).isEqualTo("agent-1");
        assertThat(d.name()).isEqualTo("name");
        assertThat(d.tenancyId()).isEqualTo("default");
        assertThat(d.slot()).isEqualTo("slot");
        assertThat(d.capabilities()).isEmpty();
    }

    @Test
    void tenancy_id_is_last_field_and_accessible() {
        var d = minimal("x", "my-tenant");
        assertThat(d.tenancyId()).isEqualTo("my-tenant");
    }

    @Test
    void capability_fields_accessible() {
        var cap = new AgentCapability("code-review", 0.9, 500L, "low",
            List.of("java"), List.of("review"), List.of("quality"), Map.of("java", 0.95));
        assertThat(cap.name()).isEqualTo("code-review");
        assertThat(cap.qualityHint()).isEqualTo(0.9);
        assertThat(cap.latencyHintP50Ms()).isEqualTo(500L);
        assertThat(cap.epistemicDomains()).containsEntry("java", 0.95);
    }

    // ── Optional field validation ──────────────────────────────────────────────

    @Test
    void version_with_bidi_throws() {
        // U+202E RIGHT-TO-LEFT OVERRIDE
        assertThatThrownBy(() -> new AgentDescriptor(
            "id", "Name", "1.0‮", null, null, null, null,
            null, null, null, "worker", List.of(), null, null, null, "tenant"))
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName())
                .isEqualTo("version"));
    }

    @Test
    void provider_blank_throws() {
        assertThatThrownBy(() -> new AgentDescriptor(
            "id", "Name", null, "  ", null, null, null,
            null, null, null, "worker", List.of(), null, null, null, "tenant"))
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName())
                .isEqualTo("provider"));
    }

    @Test
    void vocabulary_uri_exceeds_500_chars_throws() {
        assertThatThrownBy(() -> new AgentDescriptor(
            "id", "Name", null, null, null, null, null,
            "https://vocab.io/" + "x".repeat(490), null, null,
            "worker", List.of(), null, null, null, "tenant"))
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName())
                .isEqualTo("domainVocabulary"));
    }

    @Test
    void jurisdiction_with_c0_throws() {
        // U+0001 START OF HEADING — a C0 control character
        String jurisdictionWithC0 = "EU" + (char) 0x0001 + "inject";
        assertThatThrownBy(() -> new AgentDescriptor(
            "id", "Name", null, null, null, null, null,
            null, null, null, "worker", List.of(), null,
            jurisdictionWithC0, null, "tenant"))
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName())
                .isEqualTo("jurisdiction"));
    }

    @Test
    void data_handling_policy_null_is_allowed() {
        assertThatNoException().isThrownBy(() -> new AgentDescriptor(
            "id", "Name", null, null, null, null, null,
            null, null, null, "worker", List.of(), null, null, null, "tenant"));
    }

    @Test
    void weights_fingerprint_at_255_chars_is_valid() {
        assertThatNoException().isThrownBy(() -> new AgentDescriptor(
            "id", "Name", null, null, null, null, "f".repeat(255),
            null, null, null, "worker", List.of(), null, null, null, "tenant"));
    }

    @Test
    void weights_fingerprint_at_256_chars_throws() {
        assertThatThrownBy(() -> new AgentDescriptor(
            "id", "Name", null, null, null, null, "f".repeat(256),
            null, null, null, "worker", List.of(), null, null, null, "tenant"))
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName())
                .isEqualTo("weightsFingerprint"));
    }
}

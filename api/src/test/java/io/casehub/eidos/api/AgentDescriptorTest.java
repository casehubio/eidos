package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class AgentDescriptorTest {

    static AgentDescriptor minimal(String agentId, String tenancyId) {
        return AgentDescriptor.builder()
            .agentId(agentId).name("name").version("1.0").provider("provider")
            .modelFamily("modelFamily").modelVersion("modelVersion")
            .slot("slot").capabilities(List.of())
            .disposition(AgentDisposition.builder()
                .socialOrient("collaborative").ruleFollowing("principled")
                .riskAppetite("measured").autonomy("semi-autonomous")
                .build())
            .tenancyId(tenancyId)
            .build();
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
        assertThatThrownBy(() -> AgentDescriptor.builder()
            .agentId("id").name("Name").version("1.0‮")
            .slot("worker").tenancyId("tenant")
            .build())
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName())
                .isEqualTo("version"));
    }

    @Test
    void provider_blank_throws() {
        assertThatThrownBy(() -> AgentDescriptor.builder()
            .agentId("id").name("Name").provider("  ")
            .slot("worker").tenancyId("tenant")
            .build())
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName())
                .isEqualTo("provider"));
    }

    @Test
    void vocabulary_uri_exceeds_500_chars_throws() {
        assertThatThrownBy(() -> AgentDescriptor.builder()
            .agentId("id").name("Name")
            .domainVocabulary("https://vocab.io/" + "x".repeat(490))
            .slot("worker").tenancyId("tenant")
            .build())
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName())
                .isEqualTo("domainVocabulary"));
    }

    @Test
    void jurisdiction_with_c0_throws() {
        // U+0001 START OF HEADING — a C0 control character
        String jurisdictionWithC0 = "EU" + (char) 0x0001 + "inject";
        assertThatThrownBy(() -> AgentDescriptor.builder()
            .agentId("id").name("Name")
            .slot("worker").tenancyId("tenant")
            .jurisdiction(jurisdictionWithC0)
            .build())
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName())
                .isEqualTo("jurisdiction"));
    }

    @Test
    void data_handling_policy_null_is_allowed() {
        assertThatNoException().isThrownBy(() -> AgentDescriptor.builder()
            .agentId("id").name("Name")
            .slot("worker").tenancyId("tenant")
            .build());
    }

    @Test
    void data_handling_policy_blank_throws() {
        assertThatThrownBy(() -> AgentDescriptor.builder()
            .agentId("id").name("Name")
            .slot("worker").tenancyId("tenant")
            .dataHandlingPolicy("  ")
            .build())
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName())
                .isEqualTo("dataHandlingPolicy"));
    }

    @Test
    void data_handling_policy_exceeds_1000_chars_throws() {
        assertThatThrownBy(() -> AgentDescriptor.builder()
            .agentId("id").name("Name")
            .slot("worker").tenancyId("tenant")
            .dataHandlingPolicy("p".repeat(1001))
            .build())
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName())
                .isEqualTo("dataHandlingPolicy"));
    }

    @Test
    void weights_fingerprint_at_255_chars_is_valid() {
        assertThatNoException().isThrownBy(() -> AgentDescriptor.builder()
            .agentId("id").name("Name")
            .weightsFingerprint("f".repeat(255))
            .slot("worker").tenancyId("tenant")
            .build());
    }

    @Test
    void weights_fingerprint_at_256_chars_throws() {
        assertThatThrownBy(() -> AgentDescriptor.builder()
            .agentId("id").name("Name")
            .weightsFingerprint("f".repeat(256))
            .slot("worker").tenancyId("tenant")
            .build())
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName())
                .isEqualTo("weightsFingerprint"));
    }

    @Test
    void model_family_exceeds_200_chars_throws() {
        assertThatThrownBy(() -> AgentDescriptor.builder()
            .agentId("id").name("Name")
            .modelFamily("f".repeat(201))
            .slot("worker").tenancyId("tenant")
            .build())
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName())
                .isEqualTo("modelFamily"));
    }

    @Test
    void model_version_exceeds_200_chars_throws() {
        assertThatThrownBy(() -> AgentDescriptor.builder()
            .agentId("id").name("Name")
            .modelVersion("v".repeat(201))
            .slot("worker").tenancyId("tenant")
            .build())
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName())
                .isEqualTo("modelVersion"));
    }

    // ── axisVocabularies validation ─────────────────────────────────────────────

    @Test
    void axis_vocabularies_null_is_allowed() {
        assertThatNoException().isThrownBy(() ->
            AgentDescriptor.builder()
                .agentId("a").name("n").slot("s").tenancyId("t")
                .axisVocabularies(null)
                .build());
    }

    @Test
    void axis_vocabularies_null_value_throws_with_field_name() {
        var map = new java.util.HashMap<DispositionAxis, String>();
        map.put(DispositionAxis.CONFLICT_MODE, null);
        assertThatThrownBy(() ->
            AgentDescriptor.builder()
                .agentId("a").name("n").slot("s").tenancyId("t")
                .axisVocabularies(map)
                .build())
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName())
                .isEqualTo("axisVocabularies[CONFLICT_MODE]"));
    }

    @Test
    void axis_vocabularies_blank_uri_throws() {
        assertThatThrownBy(() ->
            AgentDescriptor.builder()
                .agentId("a").name("n").slot("s").tenancyId("t")
                .axisVocabularies(Map.of(DispositionAxis.CONFLICT_MODE, "  "))
                .build())
            .isInstanceOf(AgentValidationException.class);
    }

    @Test
    void axis_vocabularies_is_unmodifiable_after_construction() {
        var mutable = new java.util.HashMap<DispositionAxis, String>();
        mutable.put(DispositionAxis.CONFLICT_MODE, "urn:casehub:vocab:thomas-kilmann");
        var d = AgentDescriptor.builder()
            .agentId("a").name("n").slot("s").tenancyId("t")
            .axisVocabularies(mutable)
            .build();
        assertThatThrownBy(() -> d.axisVocabularies().put(DispositionAxis.AUTONOMY, "urn:x"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    // ── vocabUriForAxis ─────────────────────────────────────────────────────────

    @Test
    void vocabUri_returns_axis_override_when_present() {
        var d = AgentDescriptor.builder()
            .agentId("a").name("n").slot("s").tenancyId("t")
            .dispositionVocabulary("urn:casehub:vocab:disc")
            .axisVocabularies(Map.of(DispositionAxis.CONFLICT_MODE, "urn:casehub:vocab:thomas-kilmann"))
            .build();
        assertThat(d.vocabUriForAxis(DispositionAxis.CONFLICT_MODE))
            .contains("urn:casehub:vocab:thomas-kilmann");
        assertThat(d.vocabUriForAxis(DispositionAxis.SOCIAL_ORIENTATION))
            .contains("urn:casehub:vocab:disc");
    }

    @Test
    void vocabUri_falls_through_to_dispositionVocabulary_when_axis_absent() {
        var d = AgentDescriptor.builder()
            .agentId("a").name("n").slot("s").tenancyId("t")
            .dispositionVocabulary("urn:casehub:vocab:disc")
            .axisVocabularies(Map.of())
            .build();
        assertThat(d.vocabUriForAxis(DispositionAxis.CONFLICT_MODE))
            .contains("urn:casehub:vocab:disc");
    }

    @Test
    void vocabUri_falls_through_to_domainVocabulary_when_disposition_null() {
        var d = AgentDescriptor.builder()
            .agentId("a").name("n").slot("s").tenancyId("t")
            .domainVocabulary("urn:casehub:vocab:conscientiousness")
            .build();
        assertThat(d.vocabUriForAxis(DispositionAxis.SOCIAL_ORIENTATION))
            .contains("urn:casehub:vocab:conscientiousness");
    }

    @Test
    void vocabUri_returns_empty_when_all_vocab_fields_null() {
        var d = minimal("a", "t");
        assertThat(d.vocabUriForAxis(DispositionAxis.SOCIAL_ORIENTATION)).isEmpty();
    }

    @Test
    void vocabUri_returns_dispositionVocabulary_when_axisVocabularies_null() {
        var d = AgentDescriptor.builder()
            .agentId("a").name("n").slot("s").tenancyId("t")
            .dispositionVocabulary("urn:casehub:vocab:disc")
            .axisVocabularies(null)
            .build();
        assertThat(d.vocabUriForAxis(DispositionAxis.CONFLICT_MODE))
            .contains("urn:casehub:vocab:disc");
    }

    @Test
    void builder_with_explicit_nulls_equals_builder_with_nulls_omitted() {
        var explicit = AgentDescriptor.builder()
            .agentId("agent-1").name("name").version("1.0").provider("provider")
            .modelFamily("modelFamily").modelVersion("modelVersion")
            .weightsFingerprint(null)
            .domainVocabulary(null).slotVocabulary(null).dispositionVocabulary(null)
            .axisVocabularies(null)          // null → compact constructor skips Map.copyOf
            .slot("slot").capabilities(List.of())
            .disposition(AgentDisposition.builder()
                .socialOrient("collaborative").ruleFollowing("principled")
                .riskAppetite("measured").autonomy("semi-autonomous")
                .build())
            .jurisdiction(null).dataHandlingPolicy(null).tenancyId("default")
            .build();
        var omitted = AgentDescriptor.builder()
            .agentId("agent-1").name("name").version("1.0").provider("provider")
            .modelFamily("modelFamily").modelVersion("modelVersion")
            .slot("slot").capabilities(List.of())
            .disposition(AgentDisposition.builder()
                .socialOrient("collaborative").ruleFollowing("principled")
                .riskAppetite("measured").autonomy("semi-autonomous")
                .build())
            .tenancyId("default")
            .build();
        assertThat(explicit).isEqualTo(omitted);
    }
}

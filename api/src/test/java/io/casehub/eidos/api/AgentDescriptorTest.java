package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        var cap = AgentCapability.builder()
            .name("code-review").qualityHint(0.9).latencyHintP50Ms(500L).costHint("low")
            .inputTypes(List.of("java")).outputTypes(List.of("review")).tags(List.of("quality"))
            .epistemicDomains(Map.of("java", 0.95)).build();
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

    // ── vocabUriForSlot ─────────────────────────────────────────────────────────

    @Test
    void vocabUriForSlot_returns_slot_vocabulary_when_set() {
        var d = AgentDescriptor.builder()
            .agentId("a").name("n").slot("s").tenancyId("t")
            .slotVocabulary("urn:casehub:vocab:belbin")
            .domainVocabulary("urn:casehub:vocab:conscientiousness")
            .build();
        assertThat(d.vocabUriForSlot()).contains("urn:casehub:vocab:belbin");
    }

    @Test
    void vocabUriForSlot_falls_through_to_domain_vocabulary_when_slot_null() {
        var d = AgentDescriptor.builder()
            .agentId("a").name("n").slot("s").tenancyId("t")
            .domainVocabulary("urn:casehub:vocab:conscientiousness")
            .build();
        assertThat(d.vocabUriForSlot()).contains("urn:casehub:vocab:conscientiousness");
    }

    @Test
    void vocabUriForSlot_returns_empty_when_both_null() {
        var d = minimal("a", "t");
        assertThat(d.vocabUriForSlot()).isEmpty();
    }

    // ── briefing field ─────────────────────────────────────────────────────────

    @Test
    void briefing_null_by_default() {
        var d = minimal("a", "t");
        assertThat(d.briefing()).isNull();
    }

    @Test
    void briefing_round_trips_through_builder() {
        var d = AgentDescriptor.builder()
            .agentId("a").name("n").slot("s").tenancyId("t")
            .briefing("Speed is a feature. Review latency is a cost.")
            .build();
        assertThat(d.briefing()).isEqualTo("Speed is a feature. Review latency is a cost.");
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
            .briefing(null)
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

    @Test
    void hasGoal_returns_true_for_existing_goal() {
        var desc = AgentDescriptor.builder()
                                  .agentId("a").name("A").slot("s").tenancyId("t")
                                  .goals(List.of(new AgentGoal("quality", "Ensure quality", GoalPriority.PRIMARY, Visibility.PUBLIC, List.of(), null)))
                                  .build();
        assertThat(desc.hasGoal("quality")).isTrue();
        assertThat(desc.hasGoal("nonexistent")).isFalse();
    }

    @Test
    void hasConstraint_returns_true_for_existing_constraint() {
        var desc = AgentDescriptor.builder()
                                  .agentId("a").name("A").slot("s").tenancyId("t")
                                  .constraints(List.of(new AgentConstraint("no-pii", "Never expose PII", Visibility.PUBLIC, ConstraintSeverity.HARD)))
                                  .build();
        assertThat(desc.hasConstraint("no-pii")).isTrue();
        assertThat(desc.hasConstraint("nonexistent")).isFalse();
    }

    @Test void toBuilder_roundtrip_preserves_all_fields() {
        var original = AgentDescriptor.builder()
                .agentId("test-agent").name("Test Agent")
                .slot("test-slot").tenancyId("test-tenant")
                .briefing("Original briefing text that is long enough")
                .build();
        var copy = original.toBuilder().build();
        assertThat(copy).isEqualTo(original);
    }

    @Test void toBuilder_allows_field_override() {
        var original = AgentDescriptor.builder()
                .agentId("test-agent").name("Test Agent")
                .slot("test-slot").tenancyId("test-tenant")
                .briefing("Original briefing text that is long enough")
                .build();
        var modified = original.toBuilder()
                .briefing("Modified briefing text that is also long enough")
                .build();
        assertThat(modified.agentId()).isEqualTo("test-agent");
        assertThat(modified.briefing()).isEqualTo("Modified briefing text that is also long enough");
    }

    @Test void toBuilder_with_null_briefing() {
        var original = AgentDescriptor.builder()
                .agentId("test-agent").name("Test Agent")
                .slot("test-slot").tenancyId("test-tenant")
                .briefing("Original briefing text that is long enough")
                .build();
        var nullBriefing = original.toBuilder().briefing(null).build();
        assertThat(nullBriefing.briefing()).isNull();
        assertThat(nullBriefing.agentId()).isEqualTo("test-agent");
    }
}

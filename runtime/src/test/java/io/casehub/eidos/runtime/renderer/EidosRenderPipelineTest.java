package io.casehub.eidos.runtime.renderer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.eidos.api.*;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import io.casehub.eidos.runtime.vocabulary.CdiVocabularyRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.casehub.eidos.api.SystemPromptRenderer.RenderFormat.*;
import static org.assertj.core.api.Assertions.assertThat;

class EidosRenderPipelineTest {

    static final ObjectMapper MAPPER = new ObjectMapper();
    EidosRenderPipeline pipeline;

    @BeforeEach
    void setUp() {
        pipeline = new EidosRenderPipeline(new CdiVocabularyRegistry(), MAPPER);
    }

    static AgentDescriptor fullDescriptor() {
        return new AgentDescriptor(
            "reviewer-1", "Code Reviewer", "1.0", "anthropic",
            "claude", "claude-3-7-sonnet", null,
            null, null, null,
            "reviewer",
            List.of(new AgentCapability("code-review", 0.95, 150L, "low",
                List.of("code"), List.of("review"), List.of(),
                Map.of("java", 0.95, "rust", 0.3))),
            new AgentDisposition("independent", "strict", "conservative", "directed", null, false),
            "EU", "gdpr-compliant", "default"
        );
    }

    static AgentPromptContext fullContext() {
        return AgentPromptContext.forFormat(MARKDOWN)
                .withGoal(new GoalContext("Review PR #42", List.of("Check style", "Check tests"), "case-123"))
                .withResources(List.of(new Resource("/src/main/java", "Source", "filesystem")))
                .withSituationalContext("Critical release branch");
    }

    // ── Payload building (Stage 1) ────────────────────────────────────────────

    @Test
    void descriptor_payload_includes_agent_id_and_name() {
        final var node = pipeline.buildDescriptorPayload(fullDescriptor());
        assertThat(node.get("agentId").asText()).isEqualTo("reviewer-1");
        assertThat(node.get("name").asText()).isEqualTo("Code Reviewer");
    }

    @Test
    void descriptor_payload_excludes_tenancy_id() {
        final var node = pipeline.buildDescriptorPayload(fullDescriptor());
        assertThat(node.has("tenancyId")).isFalse();
    }

    @Test
    void descriptor_payload_excludes_vocabulary_uris() {
        final var node = pipeline.buildDescriptorPayload(fullDescriptor());
        assertThat(node.has("slotVocabulary")).isFalse();
        assertThat(node.has("domainVocabulary")).isFalse();
        assertThat(node.has("dispositionVocabulary")).isFalse();
    }

    @Test
    void descriptor_payload_combines_model_family_and_version() {
        final var node = pipeline.buildDescriptorPayload(fullDescriptor());
        assertThat(node.get("model").asText()).isEqualTo("claude/claude-3-7-sonnet");
    }

    @Test
    void descriptor_payload_capability_includes_input_and_output_types() {
        final var node = pipeline.buildDescriptorPayload(fullDescriptor());
        final var cap = node.get("capabilities").get(0);
        assertThat(cap.get("inputTypes").get(0).asText()).isEqualTo("code");
        assertThat(cap.get("outputTypes").get(0).asText()).isEqualTo("review");
    }

    @Test
    void descriptor_payload_capability_excludes_cost_hint_and_tags() {
        final var node = pipeline.buildDescriptorPayload(fullDescriptor());
        final var cap = node.get("capabilities").get(0);
        assertThat(cap.has("costHint")).isFalse();
        assertThat(cap.has("tags")).isFalse();
    }

    @Test
    void descriptor_payload_includes_weights_fingerprint_when_set() {
        final var desc = new AgentDescriptor(
            "id", "Name", "1.0", null, null, null, "fp-abc123",
            null, null, null, "slot", List.of(), null, null, null, "t"
        );
        final var node = pipeline.buildDescriptorPayload(desc);
        assertThat(node.get("weightsFingerprint").asText()).isEqualTo("fp-abc123");
    }

    @Test
    void context_payload_includes_goal_when_present() {
        final var node = pipeline.buildContextPayload(fullContext());
        assertThat(node.get("goal").get("description").asText()).isEqualTo("Review PR #42");
    }

    @Test
    void context_payload_includes_resources_and_situational_context_for_hash() {
        // Per design: buildContextPayload includes resources and situationalContext
        // to ensure cache correctness (they affect the rendered output in Stage 3).
        // They are excluded from LLM payload in buildLlmPayload.
        final var node = pipeline.buildContextPayload(fullContext());
        assertThat(node.has("resources")).isTrue();
        assertThat(node.get("resources").get(0).get("uri").asText()).isEqualTo("/src/main/java");
        assertThat(node.has("situationalContext")).isTrue();
        assertThat(node.get("situationalContext").asText()).isEqualTo("Critical release branch");
    }

    @Test
    void context_payload_is_empty_when_no_goal() {
        final var ctx = AgentPromptContext.forFormat(MARKDOWN);
        final var node = pipeline.buildContextPayload(ctx);
        assertThat(node.isEmpty()).isTrue();
    }

    // ── Fingerprint utility ───────────────────────────────────────────────────

    @Test
    void same_fingerprint_for_same_input() {
        final String a = EidosRenderPipeline.fingerprint("hello world");
        final String b = EidosRenderPipeline.fingerprint("hello world");
        assertThat(a).isEqualTo(b);
    }

    @Test
    void different_fingerprint_for_different_input() {
        final String a = EidosRenderPipeline.fingerprint("hello");
        final String b = EidosRenderPipeline.fingerprint("world");
        assertThat(a).isNotEqualTo(b);
    }

    // ── usesEnrichment predicate ──────────────────────────────────────────────

    @Test
    void uses_enrichment_true_for_markdown() {
        assertThat(EidosRenderPipeline.usesEnrichment(MARKDOWN)).isTrue();
    }

    @Test
    void uses_enrichment_false_for_a2a_card() {
        assertThat(EidosRenderPipeline.usesEnrichment(A2A_CARD)).isFalse();
    }

    // ── buildStage1 ───────────────────────────────────────────────────────────

    @Test
    void buildStage1_returns_matching_hashes_and_key() {
        final var desc = EidosRenderPipelineTest.fullDescriptor();
        final var ctx  = EidosRenderPipelineTest.fullContext();
        final StageOneResult s1 = pipeline.buildStage1(desc, ctx);
        assertThat(s1.descriptorHash()).hasSize(16);
        assertThat(s1.contextHash()).hasSize(16);
        assertThat(s1.lookupKey()).contains(s1.descriptorHash());
        assertThat(s1.lookupKey()).contains(s1.contextHash());
        assertThat(s1.lookupKey()).contains("MARKDOWN");
    }

    @Test
    void buildStage1_is_deterministic() {
        final var desc = EidosRenderPipelineTest.fullDescriptor();
        final var ctx  = EidosRenderPipelineTest.fullContext();
        assertThat(pipeline.buildStage1(desc, ctx).lookupKey())
            .isEqualTo(pipeline.buildStage1(desc, ctx).lookupKey());
    }
}

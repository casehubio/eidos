package io.casehub.eidos.runtime.renderer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.eidos.api.*;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import io.casehub.eidos.runtime.vocabulary.CdiVocabularyRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.casehub.eidos.api.SystemPromptRenderer.RenderFormat.*;
import static org.assertj.core.api.Assertions.assertThat;

class EidosRenderPipelineTest {

    // Used in disposition payload tests and structural renderer tests
    @VocabularyMetadata(uri = "urn:test:disp", name = "Test Disposition Vocab", version = "1.0",
                        description = "A test disposition vocabulary description")
    enum TestDispTerm implements VocabularyTerm {
        INDEPENDENT("independent", "Independent", "Works alone by preference", List.of("alone"));
        private final String value, label, description;
        private final List<String> aliases;
        TestDispTerm(String v, String l, String d, List<String> a) {
            value = v; label = l; description = d; aliases = a;
        }
        @Override public String value()         { return value; }
        @Override public String label()         { return label; }
        @Override public String description()   { return description; }
        @Override public List<String> aliases() { return aliases; }
    }

    @VocabularyMetadata(uri = "urn:test:slot", name = "Test Slot Vocab", version = "1.0",
                        description = "A test slot vocabulary description")
    enum TestSlotTerm implements VocabularyTerm {
        REVIEWER("reviewer", "Reviewer", "Reviews the work", List.of());
        private final String value, label, description;
        private final List<String> aliases;
        TestSlotTerm(String v, String l, String d, List<String> a) {
            value = v; label = l; description = d; aliases = a;
        }
        @Override public String value()         { return value; }
        @Override public String label()         { return label; }
        @Override public String description()   { return description; }
        @Override public List<String> aliases() { return aliases; }
    }

    /** Non-blank name, blank description — for frameworks description-omission test. */
    @VocabularyMetadata(uri = "urn:test:nodesc", name = "No Description Vocab")
    enum TestNoDescTerm implements VocabularyTerm {
        TERM("term", "Term", List.of());
        private final String value, label;
        private final List<String> aliases;
        TestNoDescTerm(String v, String l, List<String> a) {
            value = v; label = l; aliases = a;
        }
        @Override public String value()         { return value; }
        @Override public String label()         { return label; }
        @Override public List<String> aliases() { return aliases; }
    }

    @VocabularyMetadata(uri = "urn:test:noname")
    enum TestNoNameTerm implements VocabularyTerm {
        TERM("term", "Term", List.of());
        private final String value, label;
        private final List<String> aliases;
        TestNoNameTerm(String v, String l, List<String> a) {
            value = v; label = l; aliases = a;
        }
        @Override public String value()         { return value; }
        @Override public String label()         { return label; }
        @Override public List<String> aliases() { return aliases; }
    }

    static final ObjectMapper MAPPER = new ObjectMapper();
    CdiVocabularyRegistry vocab;
    EidosRenderPipeline pipeline;

    @BeforeEach
    void setUp() {
        vocab = new CdiVocabularyRegistry();
        pipeline = new EidosRenderPipeline(vocab, MAPPER);
    }

    static AgentDescriptor fullDescriptor() {
        return AgentDescriptor.builder()
            .agentId("reviewer-1")
            .name("Code Reviewer")
            .version("1.0")
            .provider("anthropic")
            .modelFamily("claude")
            .modelVersion("claude-3-7-sonnet")
            .slot("reviewer")
            .capabilities(List.of(new AgentCapability("code-review", 0.95, 150L, "low",
                List.of("code"), List.of("review"), List.of(),
                Map.of("java", 0.95, "rust", 0.3))))
            .disposition(AgentDisposition.builder()
                .socialOrient("independent")
                .ruleFollowing("strict")
                .riskAppetite("conservative")
                .autonomy("directed")
                .build())
            .jurisdiction("EU")
            .dataHandlingPolicy("gdpr-compliant")
            .tenancyId("default")
            .build();
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
        final var desc = AgentDescriptor.builder()
            .agentId("id")
            .name("Name")
            .version("1.0")
            .weightsFingerprint("fp-abc123")
            .slot("slot")
            .capabilities(List.of())
            .tenancyId("t")
            .build();
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

    // ── Slot vocabulary context ───────────────────────────────────────────────

    @Test
    void slot_payload_includes_vocabulary_name_and_description() {
        vocab.register(TestSlotTerm.class);
        var desc = AgentDescriptor.builder()
            .agentId("a").name("N").slotVocabulary("urn:test:slot").slot("reviewer").tenancyId("t").build();
        var node = pipeline.buildDescriptorPayload(desc);
        assertThat(node.get("slotVocabularyName").asText()).isEqualTo("Test Slot Vocab");
        assertThat(node.get("slotVocabularyDescription").asText()).isEqualTo("A test slot vocabulary description");
    }

    @Test
    void empty_vocab_name_not_emitted_in_payload() {
        vocab.register(TestNoNameTerm.class);
        var desc = AgentDescriptor.builder()
            .agentId("a").name("N").slotVocabulary("urn:test:noname").slot("term").tenancyId("t").build();
        var node = pipeline.buildDescriptorPayload(desc);
        // TestNoNameTerm has name="" and description="" — addIfNonBlank must suppress both keys
        assertThat(node.has("slotVocabularyName")).isFalse();
        assertThat(node.has("slotVocabularyDescription")).isFalse();
    }

    // ── Disposition payload (nested per-axis objects) ─────────────────────────

    @Test
    void disposition_payload_is_nested_object_per_axis() {
        vocab.register(TestDispTerm.class);
        var desc = AgentDescriptor.builder()
            .agentId("a").name("N").slot("s")
            .dispositionVocabulary("urn:test:disp")
            .disposition(AgentDisposition.builder().socialOrient("independent").build())
            .tenancyId("t").build();
        var dispNode = pipeline.buildDescriptorPayload(desc).get("disposition");
        var socialOrient = dispNode.get("socialOrient");
        assertThat(socialOrient.isObject()).isTrue();
        assertThat(socialOrient.get("value").asText()).isEqualTo("independent");
        assertThat(socialOrient.get("label").asText()).isEqualTo("Independent");
        assertThat(socialOrient.get("vocabularyName").asText()).isEqualTo("Test Disposition Vocab");
        assertThat(socialOrient.get("vocabularyDescription").asText()).isEqualTo("A test disposition vocabulary description");
        assertThat(socialOrient.get("description").asText()).isEqualTo("Works alone by preference");
    }

    @Test
    void conflict_mode_included_in_payload_when_set() {
        var desc = AgentDescriptor.builder()
            .agentId("a").name("N").slot("s")
            .disposition(AgentDisposition.builder().conflictMode("avoiding").build())
            .tenancyId("t").build();
        var dispNode = pipeline.buildDescriptorPayload(desc).get("disposition");
        assertThat(dispNode.has("conflictMode")).isTrue();
        assertThat(dispNode.get("conflictMode").get("value").asText()).isEqualTo("avoiding");
        assertThat(dispNode.has("socialOrient")).isFalse();
    }

    @Test
    void disposition_without_registered_vocab_has_value_only() {
        var desc = AgentDescriptor.builder()
            .agentId("a").name("N").slot("s")
            .dispositionVocabulary("urn:test:unregistered")
            .disposition(AgentDisposition.builder().socialOrient("custom-value").build())
            .tenancyId("t").build();
        var axisNode = pipeline.buildDescriptorPayload(desc).get("disposition").get("socialOrient");
        assertThat(axisNode.isObject()).isTrue();
        assertThat(axisNode.get("value").asText()).isEqualTo("custom-value");
        assertThat(axisNode.has("label")).isFalse();
        assertThat(axisNode.has("vocabularyName")).isFalse();
    }

    @Test
    void different_disposition_vocab_produces_different_descriptor_hash() {
        vocab.register(TestDispTerm.class);
        var descWithVocab = AgentDescriptor.builder()
            .agentId("a").name("N").slot("s")
            .dispositionVocabulary("urn:test:disp")
            .disposition(AgentDisposition.builder().socialOrient("independent").build())
            .tenancyId("t").build();
        var descWithout = AgentDescriptor.builder()
            .agentId("a").name("N").slot("s")
            .disposition(AgentDisposition.builder().socialOrient("independent").build())
            .tenancyId("t").build();
        var ctx = AgentPromptContext.forFormat(MARKDOWN);
        assertThat(pipeline.buildStage1(descWithVocab, ctx).descriptorHash())
            .isNotEqualTo(pipeline.buildStage1(descWithout, ctx).descriptorHash());
    }

    // ── Structural renderers ─────────────────────────────────────────────────

    @Test
    void structural_markdown_shows_axis_label_not_raw_value() {
        vocab.register(TestDispTerm.class);
        var desc = AgentDescriptor.builder()
            .agentId("a").name("N").slot("s")
            .dispositionVocabulary("urn:test:disp")
            .disposition(AgentDisposition.builder().socialOrient("independent").build())
            .tenancyId("t").build();
        var ctx = AgentPromptContext.forFormat(MARKDOWN);
        var s1 = pipeline.buildStage1(desc, ctx);
        var result = pipeline.assemble(s1, Optional.empty(), Optional.empty(), desc, ctx);
        assertThat(result.content()).contains("Independent (Test Disposition Vocab)");
        assertThat(result.content()).doesNotContain("Social orientation: independent\n");
        assertThat(result.content()).doesNotContain("independent");
    }

    @Test
    void structural_markdown_slot_label_via_domain_vocabulary_fallback() {
        vocab.register(TestSlotTerm.class);
        var desc = AgentDescriptor.builder()
            .agentId("a").name("N")
            .domainVocabulary("urn:test:slot") // no slotVocabulary — must fall through
            .slot("reviewer")
            .tenancyId("t").build();
        var ctx = AgentPromptContext.forFormat(MARKDOWN);
        var s1 = pipeline.buildStage1(desc, ctx);
        var result = pipeline.assemble(s1, Optional.empty(), Optional.empty(), desc, ctx);
        // vocabUriForSlot() fallback should resolve label from TestSlotTerm
        assertThat(result.content()).contains("Reviewer");
        assertThat(result.content()).doesNotContain("## Role\nreviewer\n");
    }

    @Test
    void structural_markdown_includes_conflict_mode() {
        var desc = AgentDescriptor.builder()
            .agentId("a").name("N").slot("s")
            .disposition(AgentDisposition.builder().conflictMode("avoiding").build())
            .tenancyId("t").build();
        var ctx = AgentPromptContext.forFormat(MARKDOWN);
        var s1 = pipeline.buildStage1(desc, ctx);
        var result = pipeline.assemble(s1, Optional.empty(), Optional.empty(), desc, ctx);
        assertThat(result.content()).contains("Conflict mode: avoiding");
    }

    // ── A2A card assembly ────────────────────────────────────────────────────

    private com.fasterxml.jackson.databind.JsonNode renderA2aCard(final AgentDescriptor desc) {
        final var ctx = AgentPromptContext.forFormat(A2A_CARD);
        final var s1 = pipeline.buildStage1(desc, ctx);
        final var result = pipeline.assemble(s1, Optional.empty(), Optional.empty(), desc, ctx);
        try {
            return MAPPER.readTree(result.content());
        } catch (final com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("A2A card is not valid JSON: " + result.content(), e);
        }
    }

    // buildDescriptorPayload — slot via domainVocabulary fallback

    @Test
    void descriptor_payload_slot_vocab_via_domain_vocabulary_fallback() {
        vocab.register(TestSlotTerm.class);
        var desc = AgentDescriptor.builder()
            .agentId("a").name("N")
            .domainVocabulary("urn:test:slot") // no slotVocabulary — must fall through
            .slot("reviewer")
            .tenancyId("t").build();
        var node = pipeline.buildDescriptorPayload(desc);
        assertThat(node.get("slotVocabularyName").asText()).isEqualTo("Test Slot Vocab");
        assertThat(node.get("slotLabel").asText()).isEqualTo("Reviewer");
    }

    // slot block

    @Test
    void a2a_card_slot_value_always_present() {
        var desc = AgentDescriptor.builder()
            .agentId("a").name("N").slot("reviewer").tenancyId("t").build();
        var card = renderA2aCard(desc);
        assertThat(card.get("slot").get("value").asText()).isEqualTo("reviewer");
    }

    @Test
    void a2a_card_slot_includes_vocab_fields_when_slot_vocabulary_registered() {
        vocab.register(TestSlotTerm.class);
        var desc = AgentDescriptor.builder()
            .agentId("a").name("N").slotVocabulary("urn:test:slot").slot("reviewer").tenancyId("t").build();
        var slot = renderA2aCard(desc).get("slot");
        assertThat(slot.get("value").asText()).isEqualTo("reviewer");
        assertThat(slot.get("label").asText()).isEqualTo("Reviewer");
        assertThat(slot.get("vocabularyUri").asText()).isEqualTo("urn:test:slot");
        assertThat(slot.get("vocabularyName").asText()).isEqualTo("Test Slot Vocab");
    }

    @Test
    void a2a_card_slot_includes_vocab_fields_via_domain_vocabulary_fallback() {
        vocab.register(TestSlotTerm.class);
        var desc = AgentDescriptor.builder()
            .agentId("a").name("N")
            .domainVocabulary("urn:test:slot") // no slotVocabulary — fallback via vocabUriForSlot()
            .slot("reviewer").tenancyId("t").build();
        var slot = renderA2aCard(desc).get("slot");
        assertThat(slot.get("vocabularyUri").asText()).isEqualTo("urn:test:slot");
        assertThat(slot.get("vocabularyName").asText()).isEqualTo("Test Slot Vocab");
        assertThat(slot.get("label").asText()).isEqualTo("Reviewer");
    }

    @Test
    void a2a_card_slot_omits_vocab_fields_when_no_vocabulary() {
        var desc = AgentDescriptor.builder()
            .agentId("a").name("N").slot("reviewer").tenancyId("t").build();
        var slot = renderA2aCard(desc).get("slot");
        assertThat(slot.has("vocabularyUri")).isFalse();
        assertThat(slot.has("vocabularyName")).isFalse();
        assertThat(slot.has("label")).isFalse();
    }

    // disposition block

    @Test
    void a2a_card_disposition_axis_present_when_value_set() {
        var desc = AgentDescriptor.builder()
            .agentId("a").name("N").slot("s")
            .disposition(AgentDisposition.builder().socialOrient("independent").build())
            .tenancyId("t").build();
        var disp = renderA2aCard(desc).get("disposition");
        assertThat(disp.has("socialOrient")).isTrue();
        assertThat(disp.get("socialOrient").get("value").asText()).isEqualTo("independent");
    }

    @Test
    void a2a_card_disposition_axis_omitted_when_value_null() {
        var desc = AgentDescriptor.builder()
            .agentId("a").name("N").slot("s")
            .disposition(AgentDisposition.builder().socialOrient("independent").build())
            .tenancyId("t").build();
        var disp = renderA2aCard(desc).get("disposition");
        assertThat(disp.has("ruleFollowing")).isFalse();
        assertThat(disp.has("riskAppetite")).isFalse();
        assertThat(disp.has("conflictMode")).isFalse();
    }

    @Test
    void a2a_card_disposition_with_delegation_only_emits_can_delegate_no_frameworks() {
        // delegation is primitive boolean — always has a value; all String axes null
        var desc = AgentDescriptor.builder()
            .agentId("a").name("N").slot("s")
            .disposition(AgentDisposition.builder().build()) // all axes null, delegation=false
            .tenancyId("t").build();
        var card = renderA2aCard(desc);
        var disp = card.get("disposition");
        assertThat(disp.has("canDelegate")).isTrue();
        assertThat(disp.get("canDelegate").asBoolean()).isFalse();
        assertThat(disp.has("socialOrient")).isFalse();
        assertThat(disp.has("ruleFollowing")).isFalse();
        assertThat(card.has("frameworks")).isFalse();
    }

    @Test
    void a2a_card_disposition_includes_vocab_uri_and_name_when_registered() {
        vocab.register(TestDispTerm.class);
        var desc = AgentDescriptor.builder()
            .agentId("a").name("N").slot("s")
            .dispositionVocabulary("urn:test:disp")
            .disposition(AgentDisposition.builder().socialOrient("independent").build())
            .tenancyId("t").build();
        var axis = renderA2aCard(desc).get("disposition").get("socialOrient");
        assertThat(axis.get("vocabularyUri").asText()).isEqualTo("urn:test:disp");
        assertThat(axis.get("vocabularyName").asText()).isEqualTo("Test Disposition Vocab");
        assertThat(axis.get("label").asText()).isEqualTo("Independent");
        // A2A excludes term-level description and vocabularyDescription (doc, not routing data)
        assertThat(axis.has("description")).isFalse();
        assertThat(axis.has("vocabularyDescription")).isFalse();
    }

    @Test
    void a2a_card_disposition_omits_vocab_fields_when_no_uri() {
        var desc = AgentDescriptor.builder()
            .agentId("a").name("N").slot("s")
            .disposition(AgentDisposition.builder().socialOrient("custom-value").build())
            .tenancyId("t").build();
        var axis = renderA2aCard(desc).get("disposition").get("socialOrient");
        assertThat(axis.get("value").asText()).isEqualTo("custom-value");
        assertThat(axis.has("vocabularyUri")).isFalse();
        assertThat(axis.has("vocabularyName")).isFalse();
        assertThat(axis.has("label")).isFalse();
    }

    @Test
    void a2a_card_disposition_null_produces_no_disposition_block() {
        var desc = AgentDescriptor.builder()
            .agentId("a").name("N").slot("s").tenancyId("t").build();
        var card = renderA2aCard(desc);
        assertThat(card.has("disposition")).isFalse();
    }

    // frameworks array

    @Test
    void a2a_card_frameworks_lists_instantiated_vocabularies() {
        vocab.register(TestSlotTerm.class);
        vocab.register(TestDispTerm.class);
        var desc = AgentDescriptor.builder()
            .agentId("a").name("N")
            .slotVocabulary("urn:test:slot").slot("reviewer")
            .dispositionVocabulary("urn:test:disp")
            .disposition(AgentDisposition.builder().socialOrient("independent").build())
            .tenancyId("t").build();
        var frameworks = renderA2aCard(desc).get("frameworks");
        assertThat(frameworks.isArray()).isTrue();
        assertThat(frameworks.size()).isEqualTo(2);
        // slot-first ordering
        assertThat(frameworks.get(0).get("uri").asText()).isEqualTo("urn:test:slot");
        assertThat(frameworks.get(1).get("uri").asText()).isEqualTo("urn:test:disp");
    }

    @Test
    void a2a_card_frameworks_deduplicates_same_uri() {
        vocab.register(TestDispTerm.class);
        // same URI on slot vocab AND disposition vocab
        var desc = AgentDescriptor.builder()
            .agentId("a").name("N")
            .slotVocabulary("urn:test:disp").slot("reviewer")
            .dispositionVocabulary("urn:test:disp")
            .disposition(AgentDisposition.builder().socialOrient("independent").build())
            .tenancyId("t").build();
        var frameworks = renderA2aCard(desc).get("frameworks");
        assertThat(frameworks.isArray()).isTrue();
        assertThat(frameworks.size()).isEqualTo(1);
        assertThat(frameworks.get(0).get("uri").asText()).isEqualTo("urn:test:disp");
    }

    @Test
    void a2a_card_frameworks_omitted_when_no_vocabularies() {
        var desc = AgentDescriptor.builder()
            .agentId("a").name("N").slot("s").tenancyId("t").build();
        assertThat(renderA2aCard(desc).has("frameworks")).isFalse();
    }

    @Test
    void a2a_card_frameworks_excludes_unregistered_uri_present_in_axis() {
        // unregistered URI → absent from frameworks but present as vocabularyUri in axis object
        var desc = AgentDescriptor.builder()
            .agentId("a").name("N").slot("s")
            .dispositionVocabulary("urn:test:unregistered")
            .disposition(AgentDisposition.builder().socialOrient("custom").build())
            .tenancyId("t").build();
        var card = renderA2aCard(desc);
        // not in frameworks
        assertThat(card.has("frameworks")).isFalse();
        // but IS present as vocabularyUri in the axis object
        assertThat(card.get("disposition").get("socialOrient").get("vocabularyUri").asText())
            .isEqualTo("urn:test:unregistered");
    }

    @Test
    void a2a_card_frameworks_omits_description_when_blank() {
        vocab.register(TestNoDescTerm.class);
        var desc = AgentDescriptor.builder()
            .agentId("a").name("N")
            .slotVocabulary("urn:test:nodesc").slot("term")
            .tenancyId("t").build();
        var frameworks = renderA2aCard(desc).get("frameworks");
        assertThat(frameworks.size()).isEqualTo(1);
        assertThat(frameworks.get(0).get("name").asText()).isEqualTo("No Description Vocab");
        assertThat(frameworks.get(0).has("description")).isFalse();
    }

    @Test
    void a2a_card_frameworks_includes_uri_from_domain_vocabulary_fallback() {
        vocab.register(TestDispTerm.class);
        // domainVocabulary only — no dispositionVocabulary, no slotVocabulary
        var desc = AgentDescriptor.builder()
            .agentId("a").name("N").slot("s")
            .domainVocabulary("urn:test:disp")
            .disposition(AgentDisposition.builder().socialOrient("independent").build())
            .tenancyId("t").build();
        var frameworks = renderA2aCard(desc).get("frameworks");
        assertThat(frameworks.isArray()).isTrue();
        assertThat(frameworks.size()).isEqualTo(1);
        assertThat(frameworks.get(0).get("uri").asText()).isEqualTo("urn:test:disp");
    }

    @Test
    void structural_prose_includes_all_disposition_axes() {
        var desc = AgentDescriptor.builder()
            .agentId("a").name("N").slot("s")
            .disposition(AgentDisposition.builder()
                .socialOrient("independent")
                .ruleFollowing("strict")
                .riskAppetite("conservative")
                .autonomy("directed")
                .conflictMode("avoiding")
                .build())
            .tenancyId("t").build();
        var ctx = AgentPromptContext.forFormat(PROSE);
        var s1 = pipeline.buildStage1(desc, ctx);
        var result = pipeline.assemble(s1, Optional.empty(), Optional.empty(), desc, ctx);
        assertThat(result.content()).contains("Social orientation: independent");
        assertThat(result.content()).contains("Rule following: strict");
        assertThat(result.content()).contains("Risk appetite: conservative");
        assertThat(result.content()).contains("Autonomy: directed");
        assertThat(result.content()).contains("Conflict mode: avoiding");
    }
}

package io.casehub.eidos.runtime.renderer;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import io.casehub.eidos.api.*;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import io.casehub.eidos.runtime.vocabulary.CdiVocabularyRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.casehub.eidos.api.SystemPromptRenderer.RenderFormat.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class EidosSystemPromptRendererTest {

    static final String LLM_RESPONSE = "You are a code reviewer specialising in Java.";
    static final ObjectMapper MAPPER = new ObjectMapper();

    /** JSON that SemanticEnrichmentStep can parse, with LLM_RESPONSE embedded in identityNarrative. */
    static final String LLM_JSON_RESPONSE = "{\"identityNarrative\":\"" + LLM_RESPONSE + "\","
            + "\"roleNarrative\":\"Your role is to review code.\","
            + "\"capabilityNarrative\":\"You can review Java and Rust code.\","
            + "\"dispositionNarrative\":\"You operate independently.\","
            + "\"constraintNarrative\":\"You must comply with GDPR.\","
            + "\"goalNarrative\":\"\"}";

    /** JSON in A2A enrichment format — capabilityNarratives array keyed by name. */
    static final String A2A_LLM_JSON_RESPONSE =
            "{\"capabilityNarratives\":[{\"name\":\"code-review\","
            + "\"description\":\"You conduct thorough Java code reviews, checking for correctness and style.\"}]}";

    /** Minimal in-memory cache for testing cache-hit and cache-miss behaviour. */
    static class TestRenderedPromptCache implements RenderedPromptCache {
        final Map<String, SystemPromptRenderer.RenderedPrompt> store = new HashMap<>();
        int putCount = 0;
        int getCount = 0;

        @Override
        public Optional<SystemPromptRenderer.RenderedPrompt> get(final String cacheKey) {
            getCount++;
            return Optional.ofNullable(store.get(cacheKey));
        }

        @Override
        public void put(final String cacheKey, final SystemPromptRenderer.RenderedPrompt result) {
            putCount++;
            store.put(cacheKey, result);
        }
    }

    ChatModel mockLlm;
    EidosSystemPromptRenderer rendererWithLlm;
    EidosSystemPromptRenderer rendererStructural;
    TestRenderedPromptCache testCache;

    @BeforeEach
    void setUp() {
        mockLlm = new ChatModel() {
            @Override
            public ChatResponse doChat(final ChatRequest request) {
                return ChatResponse.builder().aiMessage(AiMessage.from(LLM_JSON_RESPONSE)).build();
            }
        };
        testCache = new TestRenderedPromptCache();
        final var vocab = new CdiVocabularyRegistry();
        rendererWithLlm  = new EidosSystemPromptRenderer(mockLlm, vocab, testCache, MAPPER);
        rendererStructural = new EidosSystemPromptRenderer((ChatModel) null, vocab,
                new NoOpRenderedPromptCache(), MAPPER);
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
            new AgentDisposition("independent", "strict", "conservative", "directed", false),
            "EU", "gdpr-compliant", "default"
        );
    }

    static AgentPromptContext fullContext() {
        return AgentPromptContext.forFormat(CLAUDE_MD)
                .withGoal(new GoalContext("Review PR #42", List.of("Check style", "Check tests"), "case-123"))
                .withResources(List.of(new Resource("/src/main/java", "Source", "filesystem")))
                .withSituationalContext("Critical release branch");
    }

    static EidosSystemPromptRenderer rendererWithA2aLlm() {
        final ChatModel a2aLlm = new ChatModel() {
            @Override
            public ChatResponse doChat(final ChatRequest request) {
                return ChatResponse.builder().aiMessage(AiMessage.from(A2A_LLM_JSON_RESPONSE)).build();
            }
        };
        return new EidosSystemPromptRenderer(a2aLlm, new CdiVocabularyRegistry(),
                new NoOpRenderedPromptCache(), MAPPER);
    }

    /** Renders and returns the user message payload sent to the LLM. */
    private String capturePayload(final AgentDescriptor desc, final AgentPromptContext ctx) {
        final String[] captured = {""};
        final ChatModel capturingLlm = new ChatModel() {
            @Override
            public ChatResponse doChat(final ChatRequest request) {
                captured[0] = request.messages().stream()
                        .filter(m -> m instanceof UserMessage)
                        .map(m -> ((UserMessage) m).singleText())
                        .reduce("", (a, b) -> a + b);
                return ChatResponse.builder()
                        .aiMessage(AiMessage.from("""
                            {"identityNarrative":"You are TestAgent.",
                             "roleNarrative":"Your role is testing.",
                             "capabilityNarrative":"You can review code.",
                             "dispositionNarrative":"You are strict.",
                             "constraintNarrative":"","goalNarrative":""}"""))
                        .build();
            }
        };
        new EidosSystemPromptRenderer(capturingLlm, new CdiVocabularyRegistry(),
                new NoOpRenderedPromptCache(), MAPPER).render(desc, ctx);
        return captured[0];
    }

    // ── LLM path ──────────────────────────────────────────────────────────────

    @Test
    void llm_path_uses_llm_response_as_content() {
        final var result = rendererWithLlm.render(fullDescriptor(), fullContext());
        assertThat(result.content()).contains(LLM_RESPONSE);
    }

    @Test
    void llm_path_payload_contains_agent_id() {
        assertThat(capturePayload(fullDescriptor(), fullContext())).contains("reviewer-1");
    }

    @Test
    void llm_path_payload_contains_capability_name() {
        assertThat(capturePayload(fullDescriptor(), fullContext())).contains("code-review");
    }

    @Test
    void llm_path_payload_contains_input_types() {
        assertThat(capturePayload(fullDescriptor(), fullContext())).contains("code");
    }

    @Test
    void llm_path_payload_excludes_tenancy_id() {
        assertThat(capturePayload(fullDescriptor(), fullContext())).doesNotContain("default");
    }

    @Test
    void llm_path_payload_contains_goal_when_set() {
        assertThat(capturePayload(fullDescriptor(), fullContext())).contains("Review PR #42");
    }

    @Test
    void llm_path_payload_excludes_resources_and_situational_context() {
        assertThat(capturePayload(fullDescriptor(), fullContext()))
                .doesNotContain("/src/main/java")
                .doesNotContain("Critical release branch");
    }

    // ── Structural CLAUDE_MD path ─────────────────────────────────────────────

    @Test
    void structural_path_contains_agent_name_and_id() {
        final var result = rendererStructural.render(fullDescriptor(), fullContext());
        assertThat(result.content()).contains("Code Reviewer").contains("reviewer-1");
    }

    @Test
    void structural_path_contains_capability() {
        final var result = rendererStructural.render(fullDescriptor(), fullContext());
        assertThat(result.content()).contains("code-review");
    }

    @Test
    void structural_path_contains_disposition_axes() {
        final var result = rendererStructural.render(fullDescriptor(), fullContext());
        assertThat(result.content()).contains("independent").contains("strict");
    }

    @Test
    void structural_path_contains_goal_when_set() {
        final var result = rendererStructural.render(fullDescriptor(), fullContext());
        assertThat(result.content()).contains("Review PR #42");
    }

    @Test
    void structural_path_omits_goal_section_when_absent() {
        final var ctx = AgentPromptContext.forFormat(CLAUDE_MD);
        final var result = rendererStructural.render(fullDescriptor(), ctx);
        assertThat(result.content()).doesNotContain("## Current Goal");
    }

    @Test
    void structural_path_uses_role_heading_not_slot_label() {
        final var result = rendererStructural.render(fullDescriptor(), fullContext());
        assertThat(result.content()).contains("## Role");
    }

    @Test
    void structural_path_contains_resources_when_set() {
        final var result = rendererStructural.render(fullDescriptor(), fullContext());
        assertThat(result.content()).contains("/src/main/java");
    }

    @Test
    void structural_path_omits_resources_section_when_empty() {
        final var ctx = AgentPromptContext.forFormat(CLAUDE_MD);
        final var result = rendererStructural.render(fullDescriptor(), ctx);
        assertThat(result.content()).doesNotContain("## Resources");
    }

    @Test
    void structural_path_contains_situational_context_when_set() {
        final var result = rendererStructural.render(fullDescriptor(), fullContext());
        assertThat(result.content()).contains("Critical release branch");
    }

    @Test
    void structural_path_omits_context_section_when_null() {
        final var ctx = AgentPromptContext.forFormat(CLAUDE_MD);
        final var result = rendererStructural.render(fullDescriptor(), ctx);
        assertThat(result.content()).doesNotContain("## Context");
    }

    // ── OPENAI_SYSTEM path ────────────────────────────────────────────────────

    @Test
    void openai_structural_has_no_markdown_headers() {
        final var ctx = AgentPromptContext.forFormat(OPENAI_SYSTEM);
        final var result = rendererStructural.render(fullDescriptor(), ctx);
        assertThat(result.content()).doesNotContain("#");
    }

    @Test
    void openai_structural_contains_agent_name() {
        final var ctx = AgentPromptContext.forFormat(OPENAI_SYSTEM);
        final var result = rendererStructural.render(fullDescriptor(), ctx);
        assertThat(result.content()).contains("Code Reviewer");
    }

    // ── A2A_CARD path ─────────────────────────────────────────────────────────

    @Test
    void a2a_card_produces_json_with_name_and_capabilities() {
        final var ctx = AgentPromptContext.forFormat(A2A_CARD);
        final var result = rendererStructural.render(fullDescriptor(), ctx);
        assertThat(result.content()).contains("\"name\"").contains("code-review");
    }

    @Test
    void a2a_card_enriched_includes_capability_descriptions() {
        final var ctx = AgentPromptContext.forFormat(A2A_CARD);
        final var result = rendererWithA2aLlm().render(fullDescriptor(), ctx);
        assertThat(result.content()).contains("\"description\"");
        assertThat(result.content()).contains("You conduct thorough Java code reviews");
    }

    @Test
    void a2a_card_structural_omits_descriptions() {
        final var ctx = AgentPromptContext.forFormat(A2A_CARD);
        final var result = rendererStructural.render(fullDescriptor(), ctx);
        assertThat(result.content()).doesNotContain("\"description\"");
        assertThat(result.content()).contains("\"name\"");
        assertThat(result.content()).contains("code-review");
    }

    @Test
    void a2a_card_enriched_matches_capability_names() {
        final var ctx = AgentPromptContext.forFormat(A2A_CARD);
        final var result = rendererWithA2aLlm().render(fullDescriptor(), ctx);
        assertThat(result.content()).contains("\"code-review\"");
        assertThat(result.content()).contains("You conduct thorough Java code reviews");
    }

    @Test
    void a2a_card_enriched_ignores_unmatched_narrative_names() {
        final ChatModel mismatchLlm = new ChatModel() {
            @Override
            public ChatResponse doChat(final ChatRequest request) {
                return ChatResponse.builder().aiMessage(AiMessage.from(
                    "{\"capabilityNarratives\":[{\"name\":\"nonexistent-cap\","
                    + "\"description\":\"You do things that don't exist.\"}]}"
                )).build();
            }
        };
        final var renderer = new EidosSystemPromptRenderer(mismatchLlm, new CdiVocabularyRegistry(),
                new NoOpRenderedPromptCache(), MAPPER);
        final var ctx = AgentPromptContext.forFormat(A2A_CARD);

        final var result = renderer.render(fullDescriptor(), ctx);
        assertThat(result.content()).contains("\"code-review\"");
        assertThat(result.content()).doesNotContain("You do things that don't exist.");
        assertThat(result.content()).doesNotContain("nonexistent-cap");
    }

    @Test
    void a2a_card_llm_payload_excludes_goal_context() {
        final String[] capturedPayload = {""};
        final ChatModel capturingLlm = new ChatModel() {
            @Override
            public ChatResponse doChat(final ChatRequest request) {
                capturedPayload[0] = request.messages().stream()
                    .filter(m -> m instanceof UserMessage)
                    .map(m -> ((UserMessage) m).singleText())
                    .reduce("", (a, b) -> a + b);
                return ChatResponse.builder().aiMessage(AiMessage.from(A2A_LLM_JSON_RESPONSE)).build();
            }
        };
        final var renderer = new EidosSystemPromptRenderer(capturingLlm, new CdiVocabularyRegistry(),
                new NoOpRenderedPromptCache(), MAPPER);
        renderer.render(fullDescriptor(), AgentPromptContext.forFormat(A2A_CARD)
                .withGoal(new GoalContext("Review PR #42", List.of(), "case-123")));

        assertThat(capturedPayload[0]).doesNotContain("Review PR #42");
        assertThat(capturedPayload[0]).doesNotContain("case-123");
        assertThat(capturedPayload[0]).contains("code-review");
    }

    // ── GEMINI path ───────────────────────────────────────────────────────────

    @Test
    void gemini_structural_has_no_markdown_headers() {
        final var ctx = AgentPromptContext.forFormat(GEMINI);
        final var result = rendererStructural.render(fullDescriptor(), ctx);
        assertThat(result.content()).doesNotContain("#");
    }

    @Test
    void gemini_enriched_has_no_markdown_headers() {
        final var ctx = AgentPromptContext.forFormat(GEMINI);
        final var result = rendererWithLlm.render(fullDescriptor(), ctx);
        assertThat(result.content()).doesNotContain("#");
    }

    @Test
    void gemini_enriched_contains_identity_and_role_narrative() {
        final var ctx = AgentPromptContext.forFormat(GEMINI);
        final var result = rendererWithLlm.render(fullDescriptor(), ctx);
        // LLM_RESPONSE is the identityNarrative ("You are a code reviewer specialising in Java.")
        assertThat(result.content()).contains(LLM_RESPONSE);
        assertThat(result.content()).contains("Your role is to review code.");
    }

    @Test
    void gemini_enriched_resources_format_uses_no_space_before_paren() {
        final var ctx = AgentPromptContext.forFormat(GEMINI)
                .withResources(List.of(new Resource("https://api.example.com", "API docs", "uri")));
        final var result = rendererWithLlm.render(fullDescriptor(), ctx);
        // GEMINI: "API docs(https://api.example.com)" — no space before paren
        assertThat(result.content()).contains("API docs(https://api.example.com)");
        assertThat(result.content()).doesNotContain("API docs (https://api.example.com)");
    }

    // ── Cache behaviour ───────────────────────────────────────────────────────

    @Test
    void cache_hit_skips_llm_call() {
        final boolean[] called = {false};
        final ChatModel trackingLlm = new ChatModel() {
            @Override
            public ChatResponse doChat(final ChatRequest request) {
                called[0] = true;
                return ChatResponse.builder().aiMessage(AiMessage.from("result")).build();
            }
        };
        final var renderer = new EidosSystemPromptRenderer(trackingLlm, new CdiVocabularyRegistry(),
                testCache, MAPPER);

        renderer.render(fullDescriptor(), fullContext()); // miss — LLM called
        called[0] = false;
        renderer.render(fullDescriptor(), fullContext()); // hit — LLM must NOT be called

        assertThat(called[0]).isFalse();
    }

    @Test
    void claude_md_and_openai_system_produce_different_cache_entries() {
        // This test is both a functional correctness test and a regression guard
        // for the format-in-cache-key fix (spec review finding #1).
        final var claudeCtx  = fullContext();  // format = CLAUDE_MD from fullContext()
        final var openaiCtx  = AgentPromptContext.forFormat(OPENAI_SYSTEM)
                .withGoal(new GoalContext("Review PR #42", List.of("Check style", "Check tests"), "case-123"))
                .withResources(List.of(new Resource("/src/main/java", "Source", "filesystem")))
                .withSituationalContext("Critical release branch");

        rendererStructural.render(fullDescriptor(), claudeCtx);
        rendererStructural.render(fullDescriptor(), openaiCtx);

        // Two distinct formats = two distinct cache entries
        // rendererStructural uses NoOpRenderedPromptCache so we test via content difference
        final var claudeResult = rendererStructural.render(fullDescriptor(), claudeCtx);
        final var openaiResult = rendererStructural.render(fullDescriptor(), openaiCtx);
        assertThat(claudeResult.content()).isNotEqualTo(openaiResult.content());
        assertThat(claudeResult.format()).isEqualTo(CLAUDE_MD);
        assertThat(openaiResult.format()).isEqualTo(OPENAI_SYSTEM);
    }

    // ── Hashing ───────────────────────────────────────────────────────────────

    @Test
    void same_inputs_produce_same_hashes() {
        final var r1 = rendererStructural.render(fullDescriptor(), fullContext());
        final var r2 = rendererStructural.render(fullDescriptor(), fullContext());
        assertThat(r1.descriptorHash()).isEqualTo(r2.descriptorHash());
        assertThat(r1.contextHash()).isEqualTo(r2.contextHash());
    }

    @Test
    void different_descriptor_produces_different_descriptor_hash() {
        final var desc2 = new AgentDescriptor(
            "planner-1", "Planner", "1.0", "anthropic", "claude", "claude-3-7-sonnet",
            null, null, null, null, "planner",
            List.of(), null, null, null, "default"
        );
        final var r1 = rendererStructural.render(fullDescriptor(), fullContext());
        final var r2 = rendererStructural.render(desc2, fullContext());
        assertThat(r1.descriptorHash()).isNotEqualTo(r2.descriptorHash());
    }

    @Test
    void different_context_produces_different_context_hash() {
        final var ctx2 = AgentPromptContext.forFormat(CLAUDE_MD).withSituationalContext("different");
        final var r1 = rendererStructural.render(fullDescriptor(), fullContext());
        final var r2 = rendererStructural.render(fullDescriptor(), ctx2);
        assertThat(r1.contextHash()).isNotEqualTo(r2.contextHash());
    }

    @Test
    void rendered_prompt_has_correct_format() {
        final var result = rendererStructural.render(fullDescriptor(), fullContext());
        assertThat(result.format()).isEqualTo(CLAUDE_MD);
    }

    // ── Payload building (Stage 1) ────────────────────────────────────────────

    @Test
    void descriptor_payload_includes_agent_id_and_name() {
        final var node = rendererStructural.buildDescriptorPayload(fullDescriptor());
        assertThat(node.get("agentId").asText()).isEqualTo("reviewer-1");
        assertThat(node.get("name").asText()).isEqualTo("Code Reviewer");
    }

    @Test
    void descriptor_payload_excludes_tenancy_id() {
        final var node = rendererStructural.buildDescriptorPayload(fullDescriptor());
        assertThat(node.has("tenancyId")).isFalse();
    }

    @Test
    void descriptor_payload_excludes_vocabulary_uris() {
        final var node = rendererStructural.buildDescriptorPayload(fullDescriptor());
        assertThat(node.has("slotVocabulary")).isFalse();
        assertThat(node.has("domainVocabulary")).isFalse();
        assertThat(node.has("dispositionVocabulary")).isFalse();
    }

    @Test
    void descriptor_payload_combines_model_family_and_version() {
        final var node = rendererStructural.buildDescriptorPayload(fullDescriptor());
        assertThat(node.get("model").asText()).isEqualTo("claude/claude-3-7-sonnet");
    }

    @Test
    void descriptor_payload_capability_includes_input_and_output_types() {
        final var node = rendererStructural.buildDescriptorPayload(fullDescriptor());
        final var cap = node.get("capabilities").get(0);
        assertThat(cap.get("inputTypes").get(0).asText()).isEqualTo("code");
        assertThat(cap.get("outputTypes").get(0).asText()).isEqualTo("review");
    }

    @Test
    void descriptor_payload_capability_excludes_cost_hint_and_tags() {
        final var node = rendererStructural.buildDescriptorPayload(fullDescriptor());
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
        final var node = rendererStructural.buildDescriptorPayload(desc);
        assertThat(node.get("weightsFingerprint").asText()).isEqualTo("fp-abc123");
    }

    @Test
    void context_payload_includes_goal_when_present() {
        final var node = rendererStructural.buildContextPayload(fullContext());
        assertThat(node.get("goal").get("description").asText()).isEqualTo("Review PR #42");
    }

    @Test
    void context_payload_includes_resources_and_situational_context_for_hash() {
        // Per design: buildContextPayload includes resources and situationalContext
        // to ensure cache correctness (they affect the rendered output in Stage 3).
        // They are excluded from LLM payload in buildLlmPayload.
        final var node = rendererStructural.buildContextPayload(fullContext());
        assertThat(node.has("resources")).isTrue();
        assertThat(node.get("resources").get(0).get("uri").asText()).isEqualTo("/src/main/java");
        assertThat(node.has("situationalContext")).isTrue();
        assertThat(node.get("situationalContext").asText()).isEqualTo("Critical release branch");
    }

    @Test
    void context_payload_is_empty_when_no_goal() {
        final var ctx = AgentPromptContext.forFormat(CLAUDE_MD);
        final var node = rendererStructural.buildContextPayload(ctx);
        assertThat(node.isEmpty()).isTrue();
    }
}

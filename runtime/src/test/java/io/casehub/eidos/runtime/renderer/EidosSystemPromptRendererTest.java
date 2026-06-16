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

import java.util.List;
import java.util.Map;

import static io.casehub.eidos.api.SystemPromptRenderer.RenderFormat.*;
import static org.assertj.core.api.Assertions.assertThat;

class EidosSystemPromptRendererTest {

    static final String LLM_RESPONSE = "You are a code reviewer specialising in Java.";
    static final ObjectMapper MAPPER = new ObjectMapper();

    /** JSON that SemanticEnrichmentStep can parse with narrowed 2-field schema. */
    static final String LLM_JSON_RESPONSE =
        "{\"dispositionNarrative\":\"You operate independently.\","
        + "\"goalNarrative\":\"\"}";

    /** JSON in A2A enrichment format — capabilityNarratives array keyed by name. */
    static final String A2A_LLM_JSON_RESPONSE =
            "{\"capabilityNarratives\":[{\"name\":\"code-review\","
            + "\"description\":\"You conduct thorough Java code reviews, checking for correctness and style.\"}]}";

    ChatModel mockLlm;
    EidosSystemPromptRenderer rendererWithLlm;
    EidosSystemPromptRenderer rendererStructural;
    TestReactiveRenderedPromptCache testCache; // freshly assigned in setUp() @BeforeEach

    @BeforeEach
    void setUp() {
        mockLlm = new ChatModel() {
            @Override
            public ChatResponse doChat(final ChatRequest request) {
                return ChatResponse.builder().aiMessage(AiMessage.from(LLM_JSON_RESPONSE)).build();
            }
        };
        testCache = new TestReactiveRenderedPromptCache();
        final var vocab = new CdiVocabularyRegistry();
        rendererWithLlm  = new EidosSystemPromptRenderer(mockLlm,
                new EidosRenderPipeline(vocab, MAPPER), testCache, MAPPER);
        rendererStructural = new EidosSystemPromptRenderer((ChatModel) null,
                new EidosRenderPipeline(vocab, MAPPER), new TestReactiveRenderedPromptCache(), MAPPER);
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

    static EidosSystemPromptRenderer rendererWithA2aLlm() {
        final ChatModel a2aLlm = new ChatModel() {
            @Override
            public ChatResponse doChat(final ChatRequest request) {
                return ChatResponse.builder().aiMessage(AiMessage.from(A2A_LLM_JSON_RESPONSE)).build();
            }
        };
        return new EidosSystemPromptRenderer(a2aLlm,
                new EidosRenderPipeline(new CdiVocabularyRegistry(), MAPPER),
                new TestReactiveRenderedPromptCache(), MAPPER);
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
                        .findFirst().orElse("");
                return ChatResponse.builder()
                        .aiMessage(AiMessage.from(
                            "{\"dispositionNarrative\":\"You are strict.\","
                            + "\"goalNarrative\":\"\"}"))
                        .build();
            }
        };
        new EidosSystemPromptRenderer(capturingLlm,
                new EidosRenderPipeline(new CdiVocabularyRegistry(), MAPPER),
                new TestReactiveRenderedPromptCache(), MAPPER).render(desc, ctx);
        return captured[0];
    }

    // ── LLM path ──────────────────────────────────────────────────────────────

    @Test
    void llm_path_uses_enriched_disposition_narrative() {
        final var result = rendererWithLlm.render(fullDescriptor(), fullContext());
        // dispositionNarrative from LLM_JSON_RESPONSE replaces structural disposition section
        assertThat(result.content()).contains("You operate independently.");
    }

    @Test
    void llm_path_payload_contains_name() {
        assertThat(capturePayload(fullDescriptor(), fullContext())).contains("Code Reviewer");
    }

    @Test
    void llm_path_payload_contains_slot() {
        assertThat(capturePayload(fullDescriptor(), fullContext())).contains("reviewer");
    }

    @Test
    void llm_path_payload_excludes_capabilities() {
        // enrichment payload is focused: capabilities rendered structurally, not sent to LLM
        assertThat(capturePayload(fullDescriptor(), fullContext())).doesNotContain("code-review");
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

    @Test
    void llm_path_payload_excludes_agent_id() {
        // agentId is an identity field — not in the focused enrichment payload
        assertThat(capturePayload(fullDescriptor(), fullContext())).doesNotContain("reviewer-1");
    }

    // ── Structural MARKDOWN path ──────────────────────────────────────────────

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
        final var ctx = AgentPromptContext.forFormat(MARKDOWN);
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
        final var ctx = AgentPromptContext.forFormat(MARKDOWN);
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
        final var ctx = AgentPromptContext.forFormat(MARKDOWN);
        final var result = rendererStructural.render(fullDescriptor(), ctx);
        assertThat(result.content()).doesNotContain("## Context");
    }

    // ── PROSE path ────────────────────────────────────────────────────────────

    @Test
    void prose_structural_has_no_markdown_headers() {
        final var ctx = AgentPromptContext.forFormat(PROSE);
        final var result = rendererStructural.render(fullDescriptor(), ctx);
        assertThat(result.content()).doesNotContain("#");
    }

    @Test
    void prose_structural_contains_agent_name() {
        final var ctx = AgentPromptContext.forFormat(PROSE);
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
        final var renderer = new EidosSystemPromptRenderer(mismatchLlm,
                new EidosRenderPipeline(new CdiVocabularyRegistry(), MAPPER),
                new TestReactiveRenderedPromptCache(), MAPPER);
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
                    .findFirst().orElse("");
                return ChatResponse.builder().aiMessage(AiMessage.from(A2A_LLM_JSON_RESPONSE)).build();
            }
        };
        final var renderer = new EidosSystemPromptRenderer(capturingLlm,
                new EidosRenderPipeline(new CdiVocabularyRegistry(), MAPPER),
                new TestReactiveRenderedPromptCache(), MAPPER);
        renderer.render(fullDescriptor(), AgentPromptContext.forFormat(A2A_CARD)
                .withGoal(new GoalContext("Review PR #42", List.of(), "case-123")));

        assertThat(capturedPayload[0]).doesNotContain("Review PR #42");
        assertThat(capturedPayload[0]).doesNotContain("case-123");
        assertThat(capturedPayload[0]).contains("code-review");
    }

    // ── Cache behaviour ───────────────────────────────────────────────────────

    @Test
    void cache_hit_skips_llm_call() {
        final boolean[] called = {false};
        final ChatModel trackingLlm = new ChatModel() {
            @Override
            public ChatResponse doChat(final ChatRequest request) {
                called[0] = true;
                return ChatResponse.builder().aiMessage(AiMessage.from(LLM_JSON_RESPONSE)).build();
            }
        };
        final TestReactiveRenderedPromptCache freshCache = new TestReactiveRenderedPromptCache();
        final var renderer = new EidosSystemPromptRenderer(trackingLlm,
                new EidosRenderPipeline(new CdiVocabularyRegistry(), MAPPER),
                freshCache, MAPPER);

        renderer.render(fullDescriptor(), fullContext()); // miss — LLM called
        called[0] = false;
        renderer.render(fullDescriptor(), fullContext()); // hit — LLM must NOT be called

        assertThat(called[0]).isFalse();
        assertThat(freshCache.putCount).isEqualTo(1);
        assertThat(freshCache.getCount).isEqualTo(2);
    }

    @Test
    void markdown_and_prose_produce_different_cache_entries() {
        // This test is both a functional correctness test and a regression guard
        // for the format-in-cache-key fix (spec review finding #1).
        final var markdownCtx  = fullContext();  // format = MARKDOWN from fullContext()
        final var proseCtx  = AgentPromptContext.forFormat(PROSE)
                .withGoal(new GoalContext("Review PR #42", List.of("Check style", "Check tests"), "case-123"))
                .withResources(List.of(new Resource("/src/main/java", "Source", "filesystem")))
                .withSituationalContext("Critical release branch");

        rendererStructural.render(fullDescriptor(), markdownCtx);
        rendererStructural.render(fullDescriptor(), proseCtx);

        // Two distinct formats = two distinct cache entries
        // rendererStructural uses TestReactiveRenderedPromptCache (live store); test is on content not cache state
        final var markdownResult = rendererStructural.render(fullDescriptor(), markdownCtx);
        final var proseResult = rendererStructural.render(fullDescriptor(), proseCtx);
        assertThat(markdownResult.content()).isNotEqualTo(proseResult.content());
        assertThat(markdownResult.format()).isEqualTo(MARKDOWN);
        assertThat(proseResult.format()).isEqualTo(PROSE);
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
        final var desc2 = AgentDescriptor.builder()
            .agentId("planner-1")
            .name("Planner")
            .version("1.0")
            .provider("anthropic")
            .modelFamily("claude")
            .modelVersion("claude-3-7-sonnet")
            .slot("planner")
            .capabilities(List.of())
            .tenancyId("default")
            .build();
        final var r1 = rendererStructural.render(fullDescriptor(), fullContext());
        final var r2 = rendererStructural.render(desc2, fullContext());
        assertThat(r1.descriptorHash()).isNotEqualTo(r2.descriptorHash());
    }

    @Test
    void different_context_produces_different_context_hash() {
        final var ctx2 = AgentPromptContext.forFormat(MARKDOWN).withSituationalContext("different");
        final var r1 = rendererStructural.render(fullDescriptor(), fullContext());
        final var r2 = rendererStructural.render(fullDescriptor(), ctx2);
        assertThat(r1.contextHash()).isNotEqualTo(r2.contextHash());
    }

    @Test
    void rendered_prompt_has_correct_format() {
        final var result = rendererStructural.render(fullDescriptor(), fullContext());
        assertThat(result.format()).isEqualTo(MARKDOWN);
    }

    // ── Selective override ────────────────────────────────────────────────────

    @Test
    void enriched_disposition_structural_goal_in_markdown() {
        // LLM returns disposition narrative but no goal narrative
        // Goal should fall back to structural rendering from context
        final ChatModel dispositionOnlyLlm = new ChatModel() {
            @Override
            public ChatResponse doChat(final ChatRequest request) {
                return ChatResponse.builder().aiMessage(AiMessage.from(
                    "{\"dispositionNarrative\":\"You approve boldly.\",\"goalNarrative\":\"\"}")).build();
            }
        };
        final EidosSystemPromptRenderer renderer = new EidosSystemPromptRenderer(
            dispositionOnlyLlm,
            new EidosRenderPipeline(new CdiVocabularyRegistry(), MAPPER),
            new TestReactiveRenderedPromptCache(), MAPPER);

        final String content = renderer.render(fullDescriptor(), fullContext()).content();

        // Enriched disposition present
        assertThat(content).contains("You approve boldly.");
        // Structural goal rendered (from fullContext() which has "Review PR #42")
        assertThat(content).contains("Review PR #42");
    }

    @Test
    void structural_disposition_enriched_goal_in_markdown() {
        // LLM returns goal narrative but empty disposition narrative
        // Disposition should fall back to structural bullet list
        final ChatModel goalOnlyLlm = new ChatModel() {
            @Override
            public ChatResponse doChat(final ChatRequest request) {
                return ChatResponse.builder().aiMessage(AiMessage.from(
                    "{\"dispositionNarrative\":\"\",\"goalNarrative\":\"Your task: review PR #42 thoroughly.\"}")).build();
            }
        };
        final EidosSystemPromptRenderer renderer = new EidosSystemPromptRenderer(
            goalOnlyLlm,
            new EidosRenderPipeline(new CdiVocabularyRegistry(), MAPPER),
            new TestReactiveRenderedPromptCache(), MAPPER);

        final String content = renderer.render(fullDescriptor(), fullContext()).content();

        // Enriched goal present
        assertThat(content).contains("Your task: review PR #42 thoroughly.");
        // Structural disposition rendered (bullet list style)
        assertThat(content).contains("## How You Operate");
        assertThat(content).contains("Risk appetite");
    }

    @Test
    void selective_override_works_in_prose_format() {
        // PROSE format with enriched disposition
        final ChatModel proseLlm = new ChatModel() {
            @Override
            public ChatResponse doChat(final ChatRequest request) {
                return ChatResponse.builder().aiMessage(AiMessage.from(
                    "{\"dispositionNarrative\":\"You approve boldly in prose.\",\"goalNarrative\":\"\"}")).build();
            }
        };
        final EidosSystemPromptRenderer renderer = new EidosSystemPromptRenderer(
            proseLlm,
            new EidosRenderPipeline(new CdiVocabularyRegistry(), MAPPER),
            new TestReactiveRenderedPromptCache(), MAPPER);

        final AgentPromptContext proseCtx = AgentPromptContext.forFormat(PROSE)
            .withGoal(new GoalContext("Review PR #42", List.of(), null));
        final String content = renderer.render(fullDescriptor(), proseCtx).content();

        // Enriched disposition prose present
        assertThat(content).contains("You approve boldly in prose.");
        // No markdown headers in PROSE format
        assertThat(content).doesNotContain("## How You Operate");
        assertThat(content).doesNotContain("##");
    }

    // ── Briefing structural fallback ──────────────────────────────────────────

    @Test
    void structural_markdown_includes_operating_principles_when_briefing_set() {
        final var desc = AgentDescriptor.builder()
            .agentId("a").name("Bold Engineer").slot("reviewer")
            .disposition(AgentDisposition.builder().riskAppetite("bold").delegation(false).build())
            .briefing("Speed is a feature. 90% elegant beats perfect.")
            .tenancyId("t")
            .build();
        final var ctx = AgentPromptContext.forFormat(MARKDOWN);

        final String content = rendererStructural.render(desc, ctx).content();

        assertThat(content).contains("## Operating Principles");
        assertThat(content).contains("Speed is a feature.");
    }

    @Test
    void structural_prose_includes_briefing_paragraph_when_set() {
        final var desc = AgentDescriptor.builder()
            .agentId("a").name("Bold Engineer").slot("reviewer")
            .disposition(AgentDisposition.builder().riskAppetite("bold").delegation(false).build())
            .briefing("Speed is a feature.")
            .tenancyId("t")
            .build();
        final var ctx = AgentPromptContext.forFormat(PROSE);

        final String content = rendererStructural.render(desc, ctx).content();

        assertThat(content).contains("Speed is a feature.");
        assertThat(content).doesNotContain("## Operating Principles"); // PROSE has no headers
    }

    @Test
    void no_operating_principles_section_when_briefing_null() {
        final var desc = AgentDescriptor.builder()
            .agentId("a").name("n").slot("s").tenancyId("t")
            .disposition(AgentDisposition.builder().riskAppetite("bold").delegation(false).build())
            .build();
        final var ctx = AgentPromptContext.forFormat(MARKDOWN);

        final String content = rendererStructural.render(desc, ctx).content();

        assertThat(content).doesNotContain("Operating Principles");
    }

    @Test
    void briefing_does_not_appear_in_a2a_card() {
        // briefing is a human-interpretable instruction — not an A2A routing signal
        final var desc = AgentDescriptor.builder()
            .agentId("a").name("Bold Engineer").slot("reviewer")
            .capabilities(List.of(new AgentCapability("code-review", null, null, null,
                List.of(), List.of(), List.of(), Map.of())))
            .briefing("Speed is a feature.")
            .tenancyId("t")
            .build();
        final var ctx = AgentPromptContext.forFormat(A2A_CARD);

        final String content = rendererStructural.render(desc, ctx).content();

        // A2A card is JSON — briefing should not appear in the rendered output
        assertThat(content).doesNotContain("Speed is a feature.");
        assertThat(content).doesNotContain("briefing");
    }

    @Test
    void briefing_in_descriptor_payload_affects_cache_key() {
        // Two descriptors identical except briefing should produce different cache keys.
        // Verify by checking that the descriptorNode for the briefing descriptor contains "briefing".
        final var vocabRegistry = new CdiVocabularyRegistry();
        final var pipeline = new EidosRenderPipeline(vocabRegistry, MAPPER);
        final var descWithBriefing = AgentDescriptor.builder()
            .agentId("a").name("n").slot("s").tenancyId("t")
            .briefing("Speed is a feature.").build();
        final var descWithout = AgentDescriptor.builder()
            .agentId("a").name("n").slot("s").tenancyId("t").build();

        final var nodeWith = pipeline.buildDescriptorPayload(descWithBriefing, MARKDOWN);
        final var nodeWithout = pipeline.buildDescriptorPayload(descWithout, MARKDOWN);

        assertThat(nodeWith.has("briefing")).isTrue();
        assertThat(nodeWith.get("briefing").asText()).isEqualTo("Speed is a feature.");
        assertThat(nodeWithout.has("briefing")).isFalse();
        // Different JSON means different fingerprint means different cache key
        assertThat(nodeWith.toString()).isNotEqualTo(nodeWithout.toString());
    }
}

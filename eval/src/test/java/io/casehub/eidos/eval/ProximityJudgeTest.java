package io.casehub.eidos.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import io.casehub.eidos.api.*;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import io.casehub.eidos.api.SystemPromptRenderer.RenderedPrompt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProximityJudgeTest {

    static final String VALID_JSON = """
        { "score": 4, "reasoning": "Core role captured.", "gaps": ["philosophy not expressed"] }
        """;

    ProximityJudge judge;
    ProfiledEvalCase evalCase;
    RenderedPrompt rendered;

    @BeforeEach
    void setUp() {
        judge = new ProximityJudge(new ChatModel() {
            @Override
            public ChatResponse doChat(final ChatRequest request) {
                return ChatResponse.builder().aiMessage(AiMessage.from(VALID_JSON)).build();
            }
        }, new ObjectMapper());
        evalCase = minimalCase();
        rendered = new RenderedPrompt("You are a careful engineer.", RenderFormat.MARKDOWN, "dh", "ch");
    }

    @Test
    void evaluate_parses_score() {
        assertThat(judge.evaluate(evalCase, rendered).score()).isEqualTo(4);
    }

    @Test
    void evaluate_parses_reasoning() {
        assertThat(judge.evaluate(evalCase, rendered).reasoning()).isEqualTo("Core role captured.");
    }

    @Test
    void evaluate_parses_gaps() {
        assertThat(judge.evaluate(evalCase, rendered).gaps())
            .containsExactly("philosophy not expressed");
    }

    @Test
    void evaluate_payload_contains_originalProse_not_descriptor() {
        final AtomicReference<String> capturedPayload = new AtomicReference<>();
        final ProximityJudge capturingJudge = new ProximityJudge(new ChatModel() {
            @Override
            public ChatResponse doChat(final ChatRequest request) {
                request.messages().forEach(m -> {
                    if (m instanceof dev.langchain4j.data.message.UserMessage um)
                        capturedPayload.set(um.singleText());
                });
                return ChatResponse.builder().aiMessage(AiMessage.from(VALID_JSON)).build();
            }
        }, new ObjectMapper());
        capturingJudge.evaluate(evalCase, rendered);
        assertThat(capturedPayload.get()).contains("You are careful.");  // originalProse
        assertThat(capturedPayload.get()).doesNotContain("agentId");     // no descriptor in payload
    }

    @Test
    void evaluate_throws_malformed_when_score_missing() {
        final var noScore = new ProximityJudge(new ChatModel() {
            @Override
            public ChatResponse doChat(final ChatRequest request) {
                return ChatResponse.builder().aiMessage(
                    AiMessage.from("{\"reasoning\": \"ok\", \"gaps\": []}")).build();
            }
        }, new ObjectMapper());
        assertThatThrownBy(() -> noScore.evaluate(evalCase, rendered))
            .isInstanceOf(MalformedJudgeResponseException.class);
    }

    @Test
    void evaluate_throws_malformed_when_reasoning_missing() {
        final var noReasoning = new ProximityJudge(new ChatModel() {
            @Override
            public ChatResponse doChat(final ChatRequest request) {
                return ChatResponse.builder().aiMessage(
                    AiMessage.from("{\"score\": 3, \"gaps\": []}")).build();
            }
        }, new ObjectMapper());
        assertThatThrownBy(() -> noReasoning.evaluate(evalCase, rendered))
            .isInstanceOf(MalformedJudgeResponseException.class);
    }

    private static ProfiledEvalCase minimalCase() {
        final var desc = new AgentDescriptor(
            "id", "N", null, null, null, null, null, null, null, null, null,
            "reviewer", List.of(), null, null, null, "t");
        final var profile = new AgentProfile(
            "sw-engineer-careful", "SW Eng", "sw", null, null,
            SourceType.ANTHROPIC_LIBRARY, "You are careful.", null, null,
            Map.of(), Map.of(), desc, List.of());
        return new ProfiledEvalCase("test", desc,
            AgentPromptContext.forFormat(RenderFormat.MARKDOWN), profile);
    }
}

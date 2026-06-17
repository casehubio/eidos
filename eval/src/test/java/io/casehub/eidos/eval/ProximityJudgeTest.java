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

    static final String VALID_JSON =
        "{ \"score\": 4, \"reasoning\": \"Axes clearly conveyed.\", \"gaps\": [\"no autonomy axis\"] }";

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
        evalCase = caseWithDisposition(
            AgentDisposition.builder().riskAppetite("bold").ruleFollowing("strict").build());
        rendered = new RenderedPrompt("You approve boldly.", RenderFormat.MARKDOWN, "dh", "ch", false);
    }

    @Test
    void evaluate_parses_score() {
        assertThat(judge.evaluate(evalCase, rendered).score()).isEqualTo(4);
    }

    @Test
    void evaluate_parses_reasoning() {
        assertThat(judge.evaluate(evalCase, rendered).reasoning()).isEqualTo("Axes clearly conveyed.");
    }

    @Test
    void evaluate_parses_gaps() {
        assertThat(judge.evaluate(evalCase, rendered).gaps())
            .containsExactly("no autonomy axis");
    }

    @Test
    void evaluate_payload_contains_disposition_axes_not_originalProse() {
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

        assertThat(capturedPayload.get()).contains("riskAppetite");
        assertThat(capturedPayload.get()).contains("bold");
        assertThat(capturedPayload.get()).doesNotContain("You are careful."); // no originalProse
    }

    @Test
    void null_axes_excluded_from_payload() {
        // Only riskAppetite is set; conflictMode is null — must not appear in payload
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

        assertThat(capturedPayload.get()).doesNotContain("conflictMode");
    }

    @Test
    void null_disposition_produces_no_disposition_key_in_payload() {
        final var descNoDisp = AgentDescriptor.builder()
            .agentId("id").name("N").slot("s").tenancyId("t").build();
        final var profileNoDisp = new AgentProfile(
            "p", "r", "d", null, null, SourceType.ANTHROPIC_LIBRARY,
            "prose", null, null, Map.of(), Map.of(), descNoDisp, List.of());
        final var caseNoDisp = new ProfiledEvalCase("nodisp", descNoDisp,
            AgentPromptContext.forFormat(RenderFormat.MARKDOWN), profileNoDisp);

        final AtomicReference<String> capturedPayload = new AtomicReference<>();
        final ProximityJudge capturingJudge = new ProximityJudge(new ChatModel() {
            @Override
            public ChatResponse doChat(final ChatRequest request) {
                request.messages().forEach(m -> {
                    if (m instanceof dev.langchain4j.data.message.UserMessage um)
                        capturedPayload.set(um.singleText());
                });
                return ChatResponse.builder().aiMessage(AiMessage.from(
                    "{\"score\":5,\"reasoning\":\"No disposition axes declared\",\"gaps\":[]}")).build();
            }
        }, new ObjectMapper());

        final var result = capturingJudge.evaluate(caseNoDisp, rendered);
        assertThat(capturedPayload.get()).doesNotContain("disposition");
        assertThat(result.score()).isEqualTo(5);
        assertThat(result.reasoning()).isEqualTo("No disposition axes declared");
    }

    @Test
    void non_json_response_retries_then_throws() {
        // Raw non-JSON triggers JsonProcessingException in parse, which should be retried once.
        // If both attempts return non-JSON, MalformedJudgeResponseException propagates.
        final int[] callCount = {0};
        final ProximityJudge retryJudge = new ProximityJudge(new ChatModel() {
            @Override public ChatResponse doChat(final ChatRequest r) {
                callCount[0]++;
                return ChatResponse.builder().aiMessage(AiMessage.from("not json")).build();
            }
        }, new ObjectMapper());
        assertThatThrownBy(() -> retryJudge.evaluate(evalCase, rendered))
            .isInstanceOf(MalformedJudgeResponseException.class);
        assertThat(callCount[0]).isEqualTo(2);
    }

    @Test
    void evaluate_throws_malformed_when_score_missing() {
        final var noScore = new ProximityJudge(new ChatModel() {
            @Override public ChatResponse doChat(final ChatRequest r) {
                return ChatResponse.builder().aiMessage(
                    AiMessage.from("{\"reasoning\":\"ok\",\"gaps\":[]}")).build();
            }
        }, new ObjectMapper());
        assertThatThrownBy(() -> noScore.evaluate(evalCase, rendered))
            .isInstanceOf(MalformedJudgeResponseException.class);
    }

    private static ProfiledEvalCase caseWithDisposition(final AgentDisposition disp) {
        final var desc = AgentDescriptor.builder()
            .agentId("id").name("N").slot("reviewer")
            .disposition(disp)
            .capabilities(List.of())
            .tenancyId("t")
            .build();
        final var profile = new AgentProfile(
            "sw-engineer-careful", "SW Eng", "sw", null, null,
            SourceType.ANTHROPIC_LIBRARY, "You are careful.", null, null,
            Map.of(), Map.of(), desc, List.of());
        return new ProfiledEvalCase("test", desc,
            AgentPromptContext.forFormat(RenderFormat.MARKDOWN), profile);
    }
}

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

import static org.assertj.core.api.Assertions.assertThat;

class PromptJudgeTest {

    static final String VALID_JUDGE_JSON = """
        {
          "SECOND_PERSON":    { "score": 5, "reasoning": "Uses you/your throughout." },
          "CONCISENESS":      { "score": 4, "reasoning": "Mostly concise." },
          "FACTUAL_FIDELITY": { "score": 5, "reasoning": "No hallucinated data." },
          "TONE":             { "score": 4, "reasoning": "Reads like instructions." },
          "issues": ["Minor: capability latency not mentioned"]
        }""";

    PromptJudge judge;
    EvalCase evalCase;
    RenderedPrompt rendered;

    @BeforeEach
    void setUp() {
        final ChatModel stubJudge = new ChatModel() {
            @Override
            public ChatResponse doChat(final ChatRequest request) {
                return ChatResponse.builder().aiMessage(AiMessage.from(VALID_JUDGE_JSON)).build();
            }
        };
        judge = new PromptJudge(stubJudge, new ObjectMapper());

        final var desc = new AgentDescriptor(
            "id", "Name", null, null, null, null, null, null, null, null,
            "worker",
            List.of(new AgentCapability("code-review", null, null, null,
                List.of(), List.of(), List.of(), Map.of())),
            null, null, null, "tenant");
        evalCase = new EvalCase("test", desc, AgentPromptContext.forFormat(RenderFormat.CLAUDE_MD));
        rendered = new RenderedPrompt("- **code-review**", RenderFormat.CLAUDE_MD, "dh", "ch");
    }

    @Test
    void evaluate_parses_scores_correctly() {
        final EvalResult result = judge.evaluate(evalCase, rendered);
        assertThat(result.scores().get(EvalDimension.SECOND_PERSON).score()).isEqualTo(5);
        assertThat(result.scores().get(EvalDimension.CONCISENESS).score()).isEqualTo(4);
        assertThat(result.scores().get(EvalDimension.FACTUAL_FIDELITY).score()).isEqualTo(5);
        assertThat(result.scores().get(EvalDimension.TONE).score()).isEqualTo(4);
    }

    @Test
    void evaluate_computes_correct_overall() {
        final EvalResult result = judge.evaluate(evalCase, rendered);
        assertThat(result.overall()).isEqualTo((5.0 + 4.0 + 5.0 + 4.0) / 4.0);
    }

    @Test
    void evaluate_detects_completeness_when_cap_present() {
        final EvalResult result = judge.evaluate(evalCase, rendered);
        assertThat(result.completenessPass()).isTrue();
        assertThat(result.missingCapabilities()).isEmpty();
    }

    @Test
    void evaluate_detects_missing_cap() {
        final RenderedPrompt noCaps = new RenderedPrompt("no caps here", RenderFormat.CLAUDE_MD, "dh", "ch");
        final EvalResult result = judge.evaluate(evalCase, noCaps);
        assertThat(result.completenessPass()).isFalse();
        assertThat(result.missingCapabilities()).containsExactly("code-review");
    }

    @Test
    void evaluate_always_calls_judge_regardless_of_completeness() {
        final boolean[] called = {false};
        final ChatModel trackingJudge = new ChatModel() {
            @Override
            public ChatResponse doChat(final ChatRequest request) {
                called[0] = true;
                return ChatResponse.builder().aiMessage(AiMessage.from(VALID_JUDGE_JSON)).build();
            }
        };
        final RenderedPrompt noCaps = new RenderedPrompt("no caps here", RenderFormat.CLAUDE_MD, "dh", "ch");
        new PromptJudge(trackingJudge, new ObjectMapper()).evaluate(evalCase, noCaps);
        assertThat(called[0]).isTrue();
    }

    @Test
    void evaluate_extracts_issues_list() {
        final EvalResult result = judge.evaluate(evalCase, rendered);
        assertThat(result.issues()).containsExactly("Minor: capability latency not mentioned");
    }
}

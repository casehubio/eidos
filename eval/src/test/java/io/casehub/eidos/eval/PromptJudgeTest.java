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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PromptJudgeTest {

    static final String VALID_JUDGE_JSON = """
        {
          "SECOND_PERSON":    { "score": 5, "reasoning": "Uses you/your throughout." },
          "CONCISENESS":      { "score": 4, "reasoning": "Mostly concise." },
          "FACTUAL_FIDELITY": { "score": 5, "reasoning": "No hallucinated data." },
          "TONE":             { "score": 4, "reasoning": "Reads like instructions." },
          "issues": ["Minor: capability latency not mentioned"]
        }""";

    static final String VALID_A2A_JUDGE_JSON = """
        {
          "COMPLETENESS":     { "score": 5, "reasoning": "All capabilities have descriptions." },
          "FACTUAL_FIDELITY": { "score": 4, "reasoning": "All claims grounded in descriptor." },
          "issues": []
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
        evalCase = new EvalCase("test", desc, AgentPromptContext.forFormat(RenderFormat.MARKDOWN));
        rendered = new RenderedPrompt("- **code-review**", RenderFormat.MARKDOWN, "dh", "ch");
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
        final RenderedPrompt noCaps = new RenderedPrompt("no caps here", RenderFormat.MARKDOWN, "dh", "ch");
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
        final RenderedPrompt noCaps = new RenderedPrompt("no caps here", RenderFormat.MARKDOWN, "dh", "ch");
        new PromptJudge(trackingJudge, new ObjectMapper()).evaluate(evalCase, noCaps);
        assertThat(called[0]).isTrue();
    }

    @Test
    void evaluate_extracts_issues_list() {
        final EvalResult result = judge.evaluate(evalCase, rendered);
        assertThat(result.issues()).containsExactly("Minor: capability latency not mentioned");
    }

    // ── A2A evaluation ────────────────────────────────────────────────────────

    @Test
    void evaluate_a2a_scores_only_completeness_and_factual_fidelity() {
        final ChatModel a2aStub = new ChatModel() {
            @Override
            public ChatResponse doChat(final ChatRequest request) {
                return ChatResponse.builder().aiMessage(AiMessage.from(VALID_A2A_JUDGE_JSON)).build();
            }
        };
        final var desc = new AgentDescriptor(
            "id", "Name", null, null, null, null, null, null, null, null,
            "worker",
            List.of(new AgentCapability("code-review", null, null, null,
                List.of(), List.of(), List.of(), Map.of())),
            null, null, null, "tenant");
        final var a2aCase = new EvalCase("a2a-test", desc,
            AgentPromptContext.forFormat(RenderFormat.A2A_CARD));
        final var a2aRendered = new RenderedPrompt(
            "{\"name\":\"Name\",\"agentId\":\"id\",\"capabilities\":[{\"name\":\"code-review\",\"description\":\"You can review code.\"}]}",
            RenderFormat.A2A_CARD, "dh", "ch");

        final EvalResult result = new PromptJudge(a2aStub, new ObjectMapper()).evaluate(a2aCase, a2aRendered);

        assertThat(result.scores()).containsOnlyKeys(EvalDimension.COMPLETENESS, EvalDimension.FACTUAL_FIDELITY);
        assertThat(result.scores().get(EvalDimension.COMPLETENESS).score()).isEqualTo(5);
        assertThat(result.scores().get(EvalDimension.FACTUAL_FIDELITY).score()).isEqualTo(4);
        assertThat(result.scores()).doesNotContainKey(EvalDimension.SECOND_PERSON);
        assertThat(result.scores()).doesNotContainKey(EvalDimension.TONE);
    }

    @Test
    void evaluate_a2a_completeness_pass_when_all_descriptions_present() {
        final ChatModel stub = new ChatModel() {
            @Override
            public ChatResponse doChat(final ChatRequest request) {
                return ChatResponse.builder().aiMessage(AiMessage.from(VALID_A2A_JUDGE_JSON)).build();
            }
        };
        final var desc = new AgentDescriptor(
            "id", "Name", null, null, null, null, null, null, null, null,
            "worker",
            List.of(new AgentCapability("sprint-planning", null, null, null,
                List.of(), List.of(), List.of(), Map.of())),
            null, null, null, "tenant");
        final var a2aCase = new EvalCase("a2a", desc, AgentPromptContext.forFormat(RenderFormat.A2A_CARD));
        final String cardWithDesc =
            "{\"capabilities\":[{\"name\":\"sprint-planning\",\"description\":\"You plan sprints.\"}]}";
        final var rendered = new RenderedPrompt(cardWithDesc, RenderFormat.A2A_CARD, "dh", "ch");

        final EvalResult result = new PromptJudge(stub, new ObjectMapper()).evaluate(a2aCase, rendered);

        assertThat(result.completenessPass()).isTrue();
        assertThat(result.missingCapabilities()).isEmpty();
    }

    @Test
    void evaluate_a2a_completeness_fail_when_description_absent() {
        final ChatModel stub = new ChatModel() {
            @Override
            public ChatResponse doChat(final ChatRequest request) {
                return ChatResponse.builder().aiMessage(AiMessage.from(VALID_A2A_JUDGE_JSON)).build();
            }
        };
        final var desc = new AgentDescriptor(
            "id", "Name", null, null, null, null, null, null, null, null,
            "worker",
            List.of(new AgentCapability("sprint-planning", null, null, null,
                List.of(), List.of(), List.of(), Map.of())),
            null, null, null, "tenant");
        final var a2aCase = new EvalCase("a2a", desc, AgentPromptContext.forFormat(RenderFormat.A2A_CARD));
        final String cardNoDesc = "{\"capabilities\":[{\"name\":\"sprint-planning\"}]}";
        final var rendered = new RenderedPrompt(cardNoDesc, RenderFormat.A2A_CARD, "dh", "ch");

        final EvalResult result = new PromptJudge(stub, new ObjectMapper()).evaluate(a2aCase, rendered);

        assertThat(result.completenessPass()).isFalse();
        assertThat(result.missingCapabilities()).containsExactly("sprint-planning");
    }

    @Test
    void evaluate_a2a_completeness_fail_when_description_is_blank() {
        final ChatModel stub = new ChatModel() {
            @Override
            public ChatResponse doChat(final ChatRequest request) {
                return ChatResponse.builder().aiMessage(AiMessage.from(VALID_A2A_JUDGE_JSON)).build();
            }
        };
        final var desc = new AgentDescriptor(
            "id", "Name", null, null, null, null, null, null, null, null,
            "worker",
            List.of(new AgentCapability("sprint-planning", null, null, null,
                List.of(), List.of(), List.of(), Map.of())),
            null, null, null, "tenant");
        final var a2aCase = new EvalCase("a2a", desc, AgentPromptContext.forFormat(RenderFormat.A2A_CARD));
        // description field is present but blank
        final String cardBlankDesc = "{\"capabilities\":[{\"name\":\"sprint-planning\",\"description\":\"\"}]}";
        final var rendered = new RenderedPrompt(cardBlankDesc, RenderFormat.A2A_CARD, "dh", "ch");

        final EvalResult result = new PromptJudge(stub, new ObjectMapper()).evaluate(a2aCase, rendered);

        assertThat(result.completenessPass()).isFalse();
        assertThat(result.missingCapabilities()).containsExactly("sprint-planning");
    }

    @Test
    void evaluate_a2a_no_capabilities_is_trivially_complete() {
        final ChatModel stub = new ChatModel() {
            @Override
            public ChatResponse doChat(final ChatRequest request) {
                return ChatResponse.builder().aiMessage(AiMessage.from(VALID_A2A_JUDGE_JSON)).build();
            }
        };
        final var desc = new AgentDescriptor(
            "id", "Name", null, null, null, null, null, null, null, null,
            "worker", List.of(), null, null, null, "tenant");
        final var a2aCase = new EvalCase("a2a", desc, AgentPromptContext.forFormat(RenderFormat.A2A_CARD));
        final var rendered = new RenderedPrompt(
            "{\"name\":\"Name\",\"agentId\":\"id\"}", RenderFormat.A2A_CARD, "dh", "ch");

        final EvalResult result = new PromptJudge(stub, new ObjectMapper()).evaluate(a2aCase, rendered);

        assertThat(result.completenessPass()).isTrue();
        assertThat(result.missingCapabilities()).isEmpty();
    }

    @Test
    void parseResponse_throws_when_applicable_dimension_missing() {
        final String incompleteJson = """
            {
              "SECOND_PERSON": { "score": 4, "reasoning": "ok" },
              "issues": []
            }""";
        final ChatModel stub = new ChatModel() {
            @Override
            public ChatResponse doChat(final ChatRequest request) {
                return ChatResponse.builder().aiMessage(AiMessage.from(incompleteJson)).build();
            }
        };
        final var desc = new AgentDescriptor(
            "id", "Name", null, null, null, null, null, null, null, null,
            "worker",
            List.of(new AgentCapability("code-review", null, null, null,
                List.of(), List.of(), List.of(), Map.of())),
            null, null, null, "tenant");
        final var evalCase = new EvalCase("test", desc, AgentPromptContext.forFormat(RenderFormat.MARKDOWN));
        final var rendered = new RenderedPrompt("- **code-review**", RenderFormat.MARKDOWN, "dh", "ch");

        assertThatThrownBy(() -> new PromptJudge(stub, new ObjectMapper()).evaluate(evalCase, rendered))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("missing dimension");
    }
}

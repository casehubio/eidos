package io.casehub.eidos.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static io.casehub.eidos.api.DispositionAxis.RISK_APPETITE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BehavioralJudgeTest {

    static final VariantPair PAIR = new VariantPair(
        RISK_APPETITE, "sw-engineer-bold", "sw-engineer-careful", List.of());

    static final String VALID_JSON = """
        { "higher": "A", "effectSize": 4, "reasoning": "Bold was clearly more risk-tolerant." }
        """;

    @Test
    void evaluate_correct_when_judge_says_A() {
        final var judge = new BehavioralJudge(stubModel(VALID_JSON), new ObjectMapper());
        final var result = judge.evaluate(PAIR, "What do you do?", "bold answer", "careful answer");
        assertThat(result.correct()).isTrue();
        assertThat(result.effectSize()).isEqualTo(4);
        assertThat(result.reasoning()).contains("risk-tolerant");
    }

    @Test
    void evaluate_not_correct_when_judge_says_B() {
        final var bJson = """
            { "higher": "B", "effectSize": 2, "reasoning": "Careful seemed bolder here." }
            """;
        final var judge = new BehavioralJudge(stubModel(bJson), new ObjectMapper());
        final var result = judge.evaluate(PAIR, "What do you do?", "bold answer", "careful answer");
        assertThat(result.correct()).isFalse();
    }

    @Test
    void evaluate_result_carries_pair_and_question() {
        final var judge = new BehavioralJudge(stubModel(VALID_JSON), new ObjectMapper());
        final var result = judge.evaluate(PAIR, "Scenario?", "A", "B");
        assertThat(result.pair()).isEqualTo(PAIR);
        assertThat(result.question()).isEqualTo("Scenario?");
        assertThat(result.higherResponse()).isEqualTo("A");
        assertThat(result.lowerResponse()).isEqualTo("B");
    }

    @Test
    void evaluate_throws_malformed_when_higher_field_missing() {
        final var badJson = """
            { "effectSize": 3, "reasoning": "ok" }
            """;
        final var judge = new BehavioralJudge(stubModel(badJson), new ObjectMapper());
        assertThatThrownBy(() -> judge.evaluate(PAIR, "q", "a", "b"))
            .isInstanceOf(MalformedJudgeResponseException.class)
            .hasMessageContaining("missing");
    }

    @Test
    void judge_prompt_includes_axis_description_and_responses() {
        final AtomicReference<String> capturedSystem = new AtomicReference<>();
        final var capturingModel = new ChatModel() {
            @Override public ChatResponse doChat(final ChatRequest request) {
                request.messages().forEach(m -> {
                    if (m instanceof SystemMessage sm) capturedSystem.set(sm.text());
                });
                return ChatResponse.builder().aiMessage(AiMessage.from(VALID_JSON)).build();
            }
        };
        final var judge = new BehavioralJudge(capturingModel, new ObjectMapper());
        judge.evaluate(PAIR, "question?", "response A text", "response B text");

        assertThat(capturedSystem.get())
            .contains(RISK_APPETITE.description())
            .contains("response A text")
            .contains("response B text")
            .doesNotContain("HIGH")
            .doesNotContain("LOW")
            .doesNotContain("NEUTRAL");
    }

    private static ChatModel stubModel(final String json) {
        return new ChatModel() {
            @Override public ChatResponse doChat(final ChatRequest r) {
                return ChatResponse.builder().aiMessage(AiMessage.from(json)).build();
            }
        };
    }
}

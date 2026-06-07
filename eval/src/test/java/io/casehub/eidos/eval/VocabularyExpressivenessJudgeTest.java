package io.casehub.eidos.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import io.casehub.eidos.api.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class VocabularyExpressivenessJudgeTest {

    static final String AXIS_RESPONSE = """
        { "score": 3, "reasoning": "approximates but loses nuance", "gap": "correctness-over-velocity" }
        """;

    @Test
    void evaluate_makes_four_calls_one_per_axis() {
        final AtomicInteger calls = new AtomicInteger();
        final VocabularyExpressivenessJudge judge = new VocabularyExpressivenessJudge(
            new ChatModel() {
                @Override
                public ChatResponse doChat(final ChatRequest request) {
                    calls.incrementAndGet();
                    return ChatResponse.builder().aiMessage(AiMessage.from(AXIS_RESPONSE)).build();
                }
            }, new ObjectMapper());
        judge.evaluate(minimalProfile());
        assertThat(calls.get()).isEqualTo(4);
    }

    @Test
    void evaluate_returns_scores_for_all_four_axes() {
        final VocabularyExpressivenessJudge judge = new VocabularyExpressivenessJudge(
            stubModel(), new ObjectMapper());
        final VocabularyExpressivenessResult result = judge.evaluate(minimalProfile());
        assertThat(result.expressivenessScores())
            .containsKeys("socialOrient", "ruleFollowing", "riskAppetite", "autonomy");
    }

    @Test
    void evaluate_score_3_is_not_a_weak_axis() {
        // score=3 > 2, so weakAxes should be empty
        final VocabularyExpressivenessJudge judge = new VocabularyExpressivenessJudge(
            stubModel(), new ObjectMapper());
        final VocabularyExpressivenessResult result = judge.evaluate(minimalProfile());
        assertThat(result.weakAxes()).isEmpty();
    }

    @Test
    void evaluate_marks_axes_scoring_le_2_as_weak() {
        final ChatModel lowScorer = new ChatModel() {
            @Override
            public ChatResponse doChat(final ChatRequest request) {
                return ChatResponse.builder()
                    .aiMessage(AiMessage.from("{\"score\":2,\"reasoning\":\"poor\",\"gap\":\"x\"}"))
                    .build();
            }
        };
        final VocabularyExpressivenessJudge judge = new VocabularyExpressivenessJudge(
            lowScorer, new ObjectMapper());
        final VocabularyExpressivenessResult result = judge.evaluate(minimalProfile());
        assertThat(result.weakAxes()).hasSize(4);
    }

    @Test
    void evaluate_sets_profileName() {
        final VocabularyExpressivenessJudge judge = new VocabularyExpressivenessJudge(
            stubModel(), new ObjectMapper());
        assertThat(judge.evaluate(minimalProfile()).profileName()).isEqualTo("test-profile");
    }

    private static ChatModel stubModel() {
        return new ChatModel() {
            @Override
            public ChatResponse doChat(final ChatRequest request) {
                return ChatResponse.builder().aiMessage(AiMessage.from(AXIS_RESPONSE)).build();
            }
        };
    }

    private static AgentProfile minimalProfile() {
        final var desc = new AgentDescriptor(
            "id", "N", null, null, null, null, null, null, null, null, null,
            "reviewer", List.of(), null, null, null, "t");
        return new AgentProfile("test-profile", "Test", "test", null, null,
            SourceType.PRACTITIONER, "You are a test agent.", null, null,
            Map.of(), Map.of(), desc, List.of());
    }
}

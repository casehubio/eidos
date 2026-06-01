package io.casehub.eidos.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import io.casehub.eidos.api.*;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import io.casehub.eidos.api.SystemPromptRenderer.RenderedPrompt;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PairContrastJudgeTest {

    // judge says "A" is higher (sw-engineer-bold is passed as Prompt A)
    static final String VALID_JSON = """
        { "higher": "A", "effectSize": 4, "reasoning": "Clearly more risk-tolerant." }
        """;

    @Test
    void evaluate_parses_effect_size() {
        assertThat(judge().evaluate(pair(), RenderFormat.MARKDOWN, renders()).effectSize())
            .isEqualTo(4);
    }

    @Test
    void evaluate_correctly_identified_when_A_is_higher() {
        // pair.higher()=sw-engineer-bold, judge says "A" = sw-engineer-bold render → correct
        assertThat(judge().evaluate(pair(), RenderFormat.MARKDOWN, renders()).correctlyIdentified())
            .isTrue();
    }

    @Test
    void evaluate_not_correctly_identified_when_B_is_higher() {
        // judge says "B" but "A" is the declared higher profile → wrong
        final var badJudge = new PairContrastJudge(
            new ChatModel() {
                @Override public ChatResponse doChat(final ChatRequest r) {
                    return ChatResponse.builder().aiMessage(AiMessage.from(
                        "{\"higher\":\"B\",\"effectSize\":3,\"reasoning\":\"ok\"}"
                    )).build();
                }
            }, new ObjectMapper());
        assertThat(badJudge.evaluate(pair(), RenderFormat.MARKDOWN, renders()).correctlyIdentified())
            .isFalse();
    }

    @Test
    void evaluate_throws_when_profile_slug_missing_from_renders() {
        assertThatThrownBy(() -> judge().evaluate(pair(), RenderFormat.MARKDOWN, Map.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("sw-engineer-bold");
    }

    @Test
    void evaluate_sets_primaryAxis_and_profiles() {
        final var result = judge().evaluate(pair(), RenderFormat.MARKDOWN, renders());
        assertThat(result.primaryAxis()).isEqualTo("riskAppetite");
        assertThat(result.profileHigh()).isEqualTo("sw-engineer-bold");
        assertThat(result.profileLow()).isEqualTo("sw-engineer-careful");
    }

    private static PairContrastJudge judge() {
        return new PairContrastJudge(
            new ChatModel() {
                @Override public ChatResponse doChat(final ChatRequest r) {
                    return ChatResponse.builder().aiMessage(AiMessage.from(VALID_JSON)).build();
                }
            }, new ObjectMapper());
    }

    private static VariantPair pair() {
        return new VariantPair("riskAppetite", "sw-engineer-bold", "sw-engineer-careful");
    }

    private static Map<ProfiledEvalCase, RenderedPrompt> renders() {
        return Map.of(
            profiledCase("sw-engineer-bold"),
                new RenderedPrompt("You are bold.", RenderFormat.MARKDOWN, "dh", "ch"),
            profiledCase("sw-engineer-careful"),
                new RenderedPrompt("You are careful.", RenderFormat.MARKDOWN, "dh", "ch")
        );
    }

    private static ProfiledEvalCase profiledCase(final String profileName) {
        final var desc = new AgentDescriptor(
            "id", "N", null, null, null, null, null, null, null, null,
            "reviewer", List.of(), null, null, null, "t");
        final var profile = new AgentProfile(profileName, "R", "d", null, null,
            SourceType.PRACTITIONER, "prose", null, null, Map.of(), Map.of(), desc, List.of());
        return new ProfiledEvalCase(
            profileName + "-markdown", desc, AgentPromptContext.forFormat(RenderFormat.MARKDOWN),
            profile);
    }
}

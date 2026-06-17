package io.casehub.eidos.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import io.casehub.eidos.api.*;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;

import static io.casehub.eidos.api.DispositionAxis.*;
import io.casehub.eidos.api.SystemPromptRenderer.RenderedPrompt;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TraitExpressionJudgeTest {

    static final String VALID_JSON = """
        {
          "riskAppetite": 1, "socialOrient": 2, "ruleFollowing": 5, "autonomy": 1,
          "delegation": "NO", "reasoning": "Very careful and rule-following."
        }
        """;

    @Test
    void evaluate_parses_expression_scores() {
        final var result = judge().evaluate(minimalCase(), rendered());
        assertThat(result.expressionScores()).containsEntry("riskAppetite", 1);
        assertThat(result.expressionScores()).containsEntry("ruleFollowing", 5);
    }

    @Test
    void evaluate_parses_delegation_assessment() {
        assertThat(judge().evaluate(minimalCase(), rendered()).delegationAssessment())
            .isEqualTo("NO");
    }

    @Test
    void evaluate_direction_match_high_declaration_score_5() {
        // ruleFollowing declared HIGH in expectedTraits → blind score 5 ≥ 4 → match = true
        assertThat(judge().evaluate(minimalCase(), rendered()).directionMatches())
            .containsEntry("ruleFollowing", true);
    }

    @Test
    void evaluate_direction_match_low_declaration_score_1() {
        // riskAppetite declared LOW → blind score 1 ≤ 2 → match = true
        assertThat(judge().evaluate(minimalCase(), rendered()).directionMatches())
            .containsEntry("riskAppetite", true);
    }

    @Test
    void evaluate_direction_mismatch_low_declaration_score_5() {
        // Create a case with riskAppetite declared LOW but judge returns score 5
        final String highRiskJson = """
            {
              "riskAppetite": 5, "socialOrient": 2, "ruleFollowing": 5, "autonomy": 1,
              "delegation": "NO", "reasoning": "Bold risk taker."
            }
            """;
        final var badJudge = new TraitExpressionJudge(
            new ChatModel() {
                @Override public ChatResponse doChat(final ChatRequest r) {
                    return ChatResponse.builder().aiMessage(AiMessage.from(highRiskJson)).build();
                }
            }, new ObjectMapper());
        // riskAppetite declared LOW, score 5 → score NOT ≤ 2 → match = false
        assertThat(badJudge.evaluate(minimalCase(), rendered()).directionMatches())
            .containsEntry("riskAppetite", false);
    }

    @Test
    void judge_payload_is_rendered_text_only_not_descriptor() {
        final AtomicReference<String> capturedPayload = new AtomicReference<>();
        final var capturingJudge = new TraitExpressionJudge(
            new ChatModel() {
                @Override public ChatResponse doChat(final ChatRequest request) {
                    request.messages().forEach(m -> {
                        if (m instanceof UserMessage um)
                            capturedPayload.set(um.singleText());
                    });
                    return ChatResponse.builder().aiMessage(AiMessage.from(VALID_JSON)).build();
                }
            }, new ObjectMapper());
        capturingJudge.evaluate(minimalCase(), rendered());
        // User message must be only the rendered text, not descriptor JSON
        assertThat(capturedPayload.get())
            .isEqualTo("You are a careful engineer.");
        assertThat(capturedPayload.get())
            .doesNotContain("agentId")
            .doesNotContain("tenancyId");
    }

    private static TraitExpressionJudge judge() {
        return new TraitExpressionJudge(
            new ChatModel() {
                @Override public ChatResponse doChat(final ChatRequest r) {
                    return ChatResponse.builder().aiMessage(AiMessage.from(VALID_JSON)).build();
                }
            }, new ObjectMapper());
    }

    private static RenderedPrompt rendered() {
        return new RenderedPrompt("You are a careful engineer.", RenderFormat.MARKDOWN, "dh", "ch", false);
    }

    private static ProfiledEvalCase minimalCase() {
        final var desc = AgentDescriptor.builder()
            .agentId("id")
            .name("N")
            .slot("reviewer")
            .capabilities(List.of())
            .tenancyId("t")
            .build();
        final var profile = new AgentProfile(
            "sw-engineer-careful", "SW Eng", "sw", null, null,
            SourceType.ANTHROPIC_LIBRARY, "You are careful.", null, null,
            Map.of(), Map.of(
                RISK_APPETITE, TraitPolarity.LOW,
                SOCIAL_ORIENTATION, TraitPolarity.LOW,
                RULE_FOLLOWING, TraitPolarity.HIGH,
                AUTONOMY, TraitPolarity.LOW),
            desc, List.of());
        return new ProfiledEvalCase("test", desc,
            AgentPromptContext.forFormat(RenderFormat.MARKDOWN), profile);
    }
}

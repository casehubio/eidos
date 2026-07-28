package io.casehub.eidos.eval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class FunctionActivationJudge {

    static final String JUDGE_PROMPT = """
        You are a Jungian cognitive function analyst. You will be given a response from
        an AI agent to a scenario. Identify which of the 8 Jungian cognitive functions
        the agent is PRIMARILY using in its response.

        The 8 functions are:
        - Ti (Introverted Thinking): builds internal logical frameworks, analytical
        - Te (Extraverted Thinking): applies logical organization externally, systematic
        - Fi (Introverted Feeling): evaluates through personal values, authentic
        - Fe (Extraverted Feeling): harmonizes group values, attentive to others
        - Si (Introverted Sensation): draws on past experience, proven methods
        - Se (Extraverted Sensation): focuses on immediate data, concrete, present-moment
        - Ni (Introverted Intuition): synthesizes patterns into singular insights, foresight
        - Ne (Extraverted Intuition): explores possibilities and connections

        Return ONLY raw JSON — no markdown, no code blocks:
        { "activatedFunction": "ti"|"te"|"fi"|"fe"|"si"|"se"|"ni"|"ne", "reasoning": string }
        """;

    private final ChatModel judgeModel;
    private final ObjectMapper mapper;

    @Inject
    public FunctionActivationJudge(@Any final Instance<ChatModel> models,
                                    final ObjectMapper mapper) {
        if (!models.isResolvable()) throw new IllegalStateException("ChatModel not configured.");
        this.judgeModel = models.get();
        this.mapper = mapper;
    }

    FunctionActivationJudge(final ChatModel judgeModel, final ObjectMapper mapper) {
        this.judgeModel = judgeModel;
        this.mapper = mapper;
    }

    public FunctionActivationResult evaluate(final String agentSystemPrompt,
                                              final String agentType,
                                              final List<FunctionScenario> scenarios) {
        final var activations = new ArrayList<Activation>();
        int correct = 0;

        for (final var scenario : scenarios) {
            final var agentRequest = ChatRequest.builder()
                    .messages(
                            SystemMessage.from(agentSystemPrompt),
                            UserMessage.from(scenario.prompt()))
                    .build();
            final String agentResponse = judgeModel.chat(agentRequest).aiMessage().text();

            final var judgeRequest = ChatRequest.builder()
                    .messages(
                            SystemMessage.from(JUDGE_PROMPT),
                            UserMessage.from("Scenario: " + scenario.prompt()
                                    + "\n\nAgent response:\n" + agentResponse))
                    .build();
            final String judgeResponse = judgeModel.chat(judgeRequest).aiMessage().text();

            try {
                final JsonNode root = mapper.readTree(PromptJudge.extractJson(judgeResponse));
                final String activated = root.has("activatedFunction")
                        ? root.get("activatedFunction").asText().toLowerCase() : "unknown";
                final boolean isCorrect = activated.equals(scenario.targetFunction().toLowerCase());
                if (isCorrect) correct++;
                activations.add(new Activation(scenario.targetFunction(), activated, isCorrect,
                        root.has("reasoning") ? root.get("reasoning").asText() : ""));
            } catch (final Exception e) {
                activations.add(new Activation(scenario.targetFunction(), "error", false,
                        e.getMessage()));
            }
        }

        final double taa = scenarios.isEmpty() ? 0.0 : (double) correct / scenarios.size();
        return new FunctionActivationResult(agentType, scenarios.size(), correct, taa, activations);
    }

    public record FunctionScenario(String targetFunction, String prompt) {}

    public record Activation(String targetFunction, String activatedFunction,
                             boolean correct, String reasoning) {}

    public record FunctionActivationResult(
            String agentType,
            int scenarioCount,
            int correctActivations,
            double taa,
            List<Activation> activations) {}
}

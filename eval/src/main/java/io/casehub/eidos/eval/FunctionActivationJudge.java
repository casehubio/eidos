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
import java.util.logging.Logger;

@ApplicationScoped
public class FunctionActivationJudge {

    private static final Logger LOG = Logger.getLogger(FunctionActivationJudge.class.getName());

    static final String JUDGE_PROMPT = """
                                       You are a Jungian cognitive function analyst. You will be given a response from
                                       an AI agent to a scenario. Identify which of the 8 Jungian cognitive functions
                                       the agent is PRIMARILY using in its response.
                                       
                                       The 8 functions are:
                                       - Ti (Introverted Thinking): builds internal logical frameworks from first principles; \
                                       seeks precision and internal consistency over external validation
                                       - Te (Extraverted Thinking): produces structured plans with explicit criteria and \
                                       measurable outcomes; organizes information systematically; prioritizes efficiency
                                       - Fi (Introverted Feeling): evaluates through deeply held personal values; makes \
                                       authentic value-aligned choices; prioritizes ethical consistency over approval
                                       - Fe (Extraverted Feeling): frames responses around group impact and relational \
                                       dynamics; seeks consensus; considers how decisions affect team harmony
                                       - Si (Introverted Sensation): draws on established procedures and past precedent; \
                                       follows proven methodologies; provides step-by-step approaches based on what worked before
                                       - Se (Extraverted Sensation): focuses on immediate concrete actionable data; addresses \
                                       present-moment realities; delivers practical hands-on solutions
                                       - Ni (Introverted Intuition): CONVERGES patterns into a singular strategic insight; \
                                       arrives at ONE deep conclusion or prediction; synthesizes into a unified vision; \
                                       the response NARROWS DOWN to a single answer
                                       - Ne (Extraverted Intuition): DIVERGES into multiple possibilities and connections; \
                                       brainstorms alternatives; generates several options before converging; \
                                       the response OPENS UP to explore many ideas
                                       
                                       CRITICAL DISTINCTION — Ni vs Ne:
                                       - Ni CONVERGES: the response arrives at ONE deep insight or prediction, narrowing down.
                                       - Ne DIVERGES: the response explores MULTIPLE possibilities or connections, opening up.
                                       If the response presents a single synthesized conclusion with conviction → Ni.
                                       If the response explores several alternatives, connections, or what-ifs → Ne.
                                       
                                       Return ONLY raw JSON — no markdown, no code blocks:
                                       { "activatedFunction": "ti"|"te"|"fi"|"fe"|"si"|"se"|"ni"|"ne", \
                                       "confidence": <0.0-1.0>, "reasoning": string }
                                       """;

    private final ChatModel agentModel;
    private ChatModel judgeModel;
    private final ObjectMapper mapper;

    @Inject
    public FunctionActivationJudge(@Any final Instance<ChatModel> models,
                                    final ObjectMapper mapper) {
        if (!models.isResolvable()) throw new IllegalStateException("ChatModel not configured.");
        this.agentModel = models.get();
        this.judgeModel = models.get();
        this.mapper = mapper;
    }

    FunctionActivationJudge(final ChatModel agentModel, final ChatModel judgeModel,
                            final ObjectMapper mapper) {
        this.agentModel = agentModel;
        this.judgeModel = judgeModel;
        this.mapper = mapper;
    }

    FunctionActivationJudge(final ChatModel model, final ObjectMapper mapper) {
        this(model, model, mapper);
    }

    void setJudgeModel(final ChatModel judgeModel) {
        this.judgeModel = judgeModel;
    }

    public FunctionActivationResult evaluate(final String agentSystemPrompt,
                                              final String agentType,
                                              final List<FunctionScenario> scenarios) {
        final var activations = new ArrayList<Activation>();
        int       correct     = 0;

        for (int i = 0; i < scenarios.size(); i++) {
            final var  scenario  = scenarios.get(i);
            final long callStart = System.currentTimeMillis();
            try {
                LOG.info(String.format("[%s] scenario %d/%d: target=%s — sending agent call...",
                                       agentType, i + 1, scenarios.size(), scenario.targetFunction()));

                final var agentRequest = ChatRequest.builder()
                                                    .messages(
                                                            SystemMessage.from(agentSystemPrompt),
                                                            UserMessage.from(scenario.prompt()))
                                                    .build();
                final String agentResponse = agentModel.chat(agentRequest).aiMessage().text();

                LOG.info(String.format("[%s] scenario %d/%d: agent responded (%d chars) — sending judge call...",
                                       agentType, i + 1, scenarios.size(), agentResponse.length()));

                final var judgeRequest = ChatRequest.builder()
                                                    .messages(
                                                            SystemMessage.from(JUDGE_PROMPT),
                                                            UserMessage.from("Scenario: " + scenario.prompt()
                                                                             + "\n\nAgent response:\n" + agentResponse))
                                                    .build();
                final String judgeResponse = judgeModel.chat(judgeRequest).aiMessage().text();

                final JsonNode root = mapper.readTree(PromptJudge.extractJson(judgeResponse));
                final String activated = root.has("activatedFunction")
                                         ? root.get("activatedFunction").asText().toLowerCase() : "unknown";
                final double confidence = root.has("confidence")
                                          ? root.get("confidence").asDouble(0.0) : 0.0;
                final boolean isCorrect = activated.equals(scenario.targetFunction().toLowerCase());
                if (isCorrect) {correct++;}

                final long elapsed = System.currentTimeMillis() - callStart;
                LOG.info(String.format("[%s] scenario %d/%d: target=%s, activated=%s, correct=%s, confidence=%.2f (%dms)",
                                       agentType, i + 1, scenarios.size(), scenario.targetFunction(),
                                       activated, isCorrect, confidence, elapsed));

                activations.add(new Activation(scenario.targetFunction(), activated, isCorrect,
                                               confidence,
                                               root.has("reasoning") ? root.get("reasoning").asText() : ""));
            } catch (final Exception e) {
                final long elapsed = System.currentTimeMillis() - callStart;
                LOG.warning(String.format("[%s] scenario %d/%d: ERROR after %dms — %s",
                                          agentType, i + 1, scenarios.size(), elapsed, e.getMessage()));
                activations.add(new Activation(scenario.targetFunction(), "error", false,
                                               0.0, e.toString()));
            }
        }

        final double taa = scenarios.isEmpty() ? 0.0 : (double) correct / scenarios.size();
        LOG.info(String.format("[%s] TAA=%.2f (%d/%d correct)", agentType, taa, correct, scenarios.size()));
        return new FunctionActivationResult(agentType, scenarios.size(), correct, taa, activations);}

    public record FunctionScenario(String targetFunction, String prompt) {}

    public record Activation(String targetFunction, String activatedFunction,
                             boolean correct, double confidence, String reasoning) {}

    public record FunctionActivationResult(
            String agentType,
            int scenarioCount,
            int correctActivations,
            double taa,
            List<Activation> activations) {}
}

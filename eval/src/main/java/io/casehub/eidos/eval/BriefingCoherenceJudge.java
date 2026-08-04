package io.casehub.eidos.eval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import io.casehub.eidos.api.AgentDisposition;
import io.casehub.eidos.api.DispositionValue;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class BriefingCoherenceJudge {

    private static final String JUNGIAN_VOCAB = "urn:casehub:vocab:jungian";
    private static final int[] STACK_WEIGHTS = {4, 3, 2, 1};

    static final String SYSTEM_PROMPT = """
            You are a personality coherence analyst. You will be given:
            1. A character's Jungian cognitive function stack (dominant to inferior)
            2. A character briefing text

            For each function in the stack, evaluate whether the briefing text
            reinforces or contradicts what that function expects.

            Return ONLY raw JSON — no markdown, no code blocks:
            {
              "functions": [
                {"function": "te", "coherence": 0.8,
                 "briefingSignal": "what the briefing implies for this function",
                 "dispositionExpectation": "what the function expects"},
                ...repeat for each stack function...
              ],
              "tensions": [
                {"function": "te", "briefingPhrase": "exact quote from briefing",
                 "dispositionConflict": "what it contradicts",
                 "severity": "LOW"},
                ...only include if tensions exist...
              ]
            }

            Coherence scoring:
            - 1.0 = briefing strongly reinforces the function's expected behavior
            - 0.7 = briefing is neutral or mildly supportive
            - 0.4 = briefing sends mixed signals
            - 0.1 = briefing actively contradicts the function

            Severity levels:
            - LOW = minor stylistic mismatch
            - MEDIUM = mixed signals that could confuse the LLM
            - HIGH = direct contradiction (e.g., J-type briefing reads as P)
            """;

    private final ChatModel judgeModel;
    private final ObjectMapper mapper;

    @Inject
    public BriefingCoherenceJudge(@Any Instance<ChatModel> models,
                                   ObjectMapper mapper) {
        this.judgeModel = models != null && models.isResolvable()
                ? models.get() : null;
        this.mapper = mapper;
    }

    BriefingCoherenceJudge(ChatModel model, ObjectMapper mapper) {
        this.judgeModel = model;
        this.mapper = mapper;
    }

    public CoherenceResult evaluate(String briefingText,
                                     AgentDisposition disposition,
                                     String dispositionVocabulary,
                                     String agentId) {
        if (!JUNGIAN_VOCAB.equals(dispositionVocabulary)
                || disposition == null
                || disposition.dispositionProfile().isEmpty()) {
            return new CoherenceResult(agentId, List.of(), List.of(), -1.0);
        }

        String stackDescription = formatStack(disposition);
        String userPrompt = "Function stack:\n" + stackDescription
                + "\nBriefing text:\n" + briefingText;

        ChatRequest request = ChatRequest.builder()
                .messages(SystemMessage.from(SYSTEM_PROMPT),
                        UserMessage.from(userPrompt))
                .build();

        String response = judgeModel.chat(request).aiMessage().text();
        return parse(agentId, disposition, response);
    }

    private String formatStack(AgentDisposition disposition) {
        var profile = disposition.dispositionProfile();
        String[] roles = {"Dominant", "Auxiliary", "Tertiary", "Inferior"};
        var sb = new StringBuilder();
        for (int i = 0; i < Math.min(profile.size(), 4); i++) {
            DispositionValue dv = profile.get(i);
            sb.append(roles[i]).append(": ").append(dv.term().toUpperCase());
            if (dv.weight() < 1.0) {
                sb.append(" (weight: ").append(String.format("%.2f", dv.weight())).append(")");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private CoherenceResult parse(String agentId,
                                   AgentDisposition disposition,
                                   String json) {
        try {
            JsonNode root = mapper.readTree(PromptJudge.extractJson(json));
            var functions = new ArrayList<FunctionCoherence>();
            if (root.has("functions")) {
                for (JsonNode fn : root.get("functions")) {
                    functions.add(new FunctionCoherence(
                            fn.get("function").asText(),
                            fn.get("coherence").asDouble(),
                            fn.has("briefingSignal") ? fn.get("briefingSignal").asText() : "",
                            fn.has("dispositionExpectation") ? fn.get("dispositionExpectation").asText() : ""));
                }
            }
            var tensions = new ArrayList<Tension>();
            if (root.has("tensions")) {
                for (JsonNode t : root.get("tensions")) {
                    tensions.add(new Tension(
                            t.get("function").asText(),
                            t.has("briefingPhrase") ? t.get("briefingPhrase").asText() : "",
                            t.has("dispositionConflict") ? t.get("dispositionConflict").asText() : "",
                            Severity.valueOf(t.get("severity").asText().toUpperCase())));
                }
            }
            double overall = weightedCoherence(functions);
            return new CoherenceResult(agentId, functions, tensions, overall);
        } catch (Exception e) {
            throw new MalformedJudgeResponseException(
                    "Failed to parse coherence response: " + e.getMessage());
        }
    }

    private double weightedCoherence(List<FunctionCoherence> functions) {
        if (functions.isEmpty()) return 0.0;
        double weightedSum = 0, totalWeight = 0;
        for (int i = 0; i < Math.min(functions.size(), STACK_WEIGHTS.length); i++) {
            weightedSum += functions.get(i).coherence() * STACK_WEIGHTS[i];
            totalWeight += STACK_WEIGHTS[i];
        }
        return totalWeight > 0 ? weightedSum / totalWeight : 0.0;
    }

    public record FunctionCoherence(String function, double coherence,
                                     String briefingSignal,
                                     String dispositionExpectation) {}

    public record Tension(String function, String briefingPhrase,
                          String dispositionConflict, Severity severity) {}

    public enum Severity { LOW, MEDIUM, HIGH }

    public record CoherenceResult(String agentId,
                                   List<FunctionCoherence> functions,
                                   List<Tension> tensions,
                                   double overallCoherence) {}
}

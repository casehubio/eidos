package io.casehub.eidos.eval;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.request.json.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class VocabularyExpressivenessJudge {

    static final List<String> AXES =
        List.of("socialOrient", "ruleFollowing", "riskAppetite", "autonomy");

    static final Map<String, String> AXIS_DESCRIPTIONS = Map.of(
        "socialOrient",
            "how collaborative or independent the agent is — whether it seeks input and coordinates with others or acts independently",
        "ruleFollowing",
            "how strictly the agent follows rules and conventions versus adapting its approach to context",
        "riskAppetite",
            "how risk-tolerant or risk-averse the agent is — whether it favours bold decisions under uncertainty or prioritises caution",
        "autonomy",
            "how self-directed versus directed-by-others the agent is — whether it takes initiative or waits for instruction"
    );

    static final String SYSTEM_TEMPLATE = """
        You are evaluating how precisely an open-string label (1–5 words) can express a
        personality concept found in the following system prompt.

        The concept: [%s]

        Score 1–5:
        - 5: A short label captures the nuance precisely
        - 3: A label approximates it but loses meaningful nuance
        - 1: The concept cannot be meaningfully captured in a short label

        If the score is ≤ 3, identify the specific nuance that is lost.

        Return JSON: { "score": int, "reasoning": string, "gap": string | null }
        """;

    static final ResponseFormat RESPONSE_FORMAT = ResponseFormat.builder()
        .type(ResponseFormatType.JSON)
        .jsonSchema(JsonSchema.builder()
            .name("ExpressivenessJudgment")
            .rootElement(JsonObjectSchema.builder()
                .addIntegerProperty("score", "Score 1–5")
                .addStringProperty("reasoning", "Explanation")
                .addStringProperty("gap", "Specific nuance lost, or null")
                .required("score", "reasoning")
                .build())
            .build())
        .build();

    private final ChatModel model;
    private final ObjectMapper mapper;

    @Inject
    public VocabularyExpressivenessJudge(@Any final Instance<ChatModel> models,
                                          final ObjectMapper mapper) {
        if (!models.isResolvable()) throw new IllegalStateException("ChatModel not configured.");
        this.model = models.get();
        this.mapper = mapper;
    }

    VocabularyExpressivenessJudge(final ChatModel model, final ObjectMapper mapper) {
        this.model = model;
        this.mapper = mapper;
    }

    public VocabularyExpressivenessResult evaluate(final AgentProfile profile) {
        final Map<String, Integer> scores = new LinkedHashMap<>();
        final List<String> weakAxes = new ArrayList<>();

        for (final String axis : AXES) {
            final int score = evaluateAxis(profile.originalProse(), axis);
            scores.put(axis, score);
            if (score <= 2) weakAxes.add(axis);
        }
        return new VocabularyExpressivenessResult(profile.name(), scores, weakAxes);
    }

    private int evaluateAxis(final String prose, final String axis) {
        final String systemPrompt =
            String.format(SYSTEM_TEMPLATE, AXIS_DESCRIPTIONS.get(axis));
        try {
            final var request = ChatRequest.builder()
                .messages(SystemMessage.from(systemPrompt), UserMessage.from(prose))
                .responseFormat(RESPONSE_FORMAT)
                .build();
            final var response = model.chat(request);
            return parseScore(response.aiMessage().text(), axis);
        } catch (final MalformedJudgeResponseException e) {
            throw e;
        } catch (final Exception e) {
            throw new IllegalStateException(
                "VocabularyExpressivenessJudge call failed for axis: " + axis, e);
        }
    }

    private int parseScore(final String json, final String axis) throws JsonProcessingException {
        final JsonNode root = mapper.readTree(json);
        final JsonNode score = root.get("score");
        if (score == null) throw new MalformedJudgeResponseException(
            "VocabularyExpressivenessJudge: missing score for axis " + axis);
        return score.asInt();
    }
}

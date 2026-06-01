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
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import io.casehub.eidos.api.SystemPromptRenderer.RenderedPrompt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class TraitExpressionJudge {

    static final List<String> NUMERIC_AXES =
        List.of("socialOrient", "ruleFollowing", "riskAppetite", "autonomy");

    static final String SYSTEM_PROMPT = """
        You are characterising an AI agent's personality based solely on the following
        system prompt. Do not infer from context or domain — read only what is
        explicitly or strongly implied.

        Score each axis 1–5:

        riskAppetite:
          5 = explicitly endorses bold decisions, accepts uncertainty
          3 = neutral
          1 = explicitly emphasises correctness, caution, and risk avoidance

        socialOrient:
          5 = explicitly collaborative — seeks input, coordinates before acting
          3 = neutral
          1 = explicitly independent — self-directed, minimal consultation

        ruleFollowing:
          5 = explicitly strict — follows processes, does not deviate
          3 = neutral
          1 = explicitly adaptive — comfortable bending conventions

        autonomy:
          5 = explicitly autonomous — takes initiative, decides without approval
          3 = neutral
          1 = explicitly directed — waits for instruction, seeks approval

        delegation:
          Does this prompt explicitly grant or restrict sub-agent delegation authority?
          Answer: "YES" | "NO" | "UNCERTAIN"

        Return JSON:
        { "riskAppetite": int, "socialOrient": int, "ruleFollowing": int, "autonomy": int,
          "delegation": "YES"|"NO"|"UNCERTAIN", "reasoning": string }
        """;

    static final ResponseFormat RESPONSE_FORMAT = ResponseFormat.builder()
        .type(ResponseFormatType.JSON)
        .jsonSchema(JsonSchema.builder()
            .name("TraitExpression")
            .rootElement(JsonObjectSchema.builder()
                .addIntegerProperty("riskAppetite", "1–5")
                .addIntegerProperty("socialOrient", "1–5")
                .addIntegerProperty("ruleFollowing", "1–5")
                .addIntegerProperty("autonomy", "1–5")
                .addStringProperty("delegation", "YES|NO|UNCERTAIN")
                .addStringProperty("reasoning", "Explanation")
                .required("riskAppetite", "socialOrient", "ruleFollowing", "autonomy",
                    "delegation", "reasoning")
                .build())
            .build())
        .build();

    private final ChatModel model;
    private final ObjectMapper mapper;

    @Inject
    public TraitExpressionJudge(@Any final Instance<ChatModel> models,
                                  final ObjectMapper mapper) {
        if (!models.isResolvable()) throw new IllegalStateException("ChatModel not configured.");
        this.model = models.get();
        this.mapper = mapper;
    }

    TraitExpressionJudge(final ChatModel model, final ObjectMapper mapper) {
        this.model = model;
        this.mapper = mapper;
    }

    public TraitExpressionResult evaluate(final ProfiledEvalCase evalCase,
                                           final RenderedPrompt rendered) {
        try {
            final var request = ChatRequest.builder()
                .messages(
                    SystemMessage.from(SYSTEM_PROMPT),
                    UserMessage.from(rendered.content()))   // ONLY the rendered text
                .responseFormat(RESPONSE_FORMAT)
                .build();
            final var response = model.chat(request);
            return parse(evalCase, rendered.format(), response.aiMessage().text());
        } catch (final MalformedJudgeResponseException e) {
            throw e;
        } catch (final Exception e) {
            throw new IllegalStateException("TraitExpressionJudge LLM call failed", e);
        }
    }

    private TraitExpressionResult parse(final ProfiledEvalCase evalCase,
                                         final RenderFormat format,
                                         final String json) throws JsonProcessingException {
        final JsonNode root = mapper.readTree(json);
        final Map<String, Integer> scores = new LinkedHashMap<>();
        for (final String axis : NUMERIC_AXES) {
            final JsonNode n = root.get(axis);
            if (n == null) throw new MalformedJudgeResponseException("Missing axis: " + axis);
            scores.put(axis, n.asInt());
        }
        final JsonNode del = root.get("delegation");
        if (del == null) throw new MalformedJudgeResponseException("Missing delegation");

        final Map<String, Boolean> matches = computeMatches(evalCase.profile(), scores);
        return new TraitExpressionResult(evalCase, format, scores, matches, del.asText());
    }

    private Map<String, Boolean> computeMatches(final AgentProfile profile,
                                                  final Map<String, Integer> scores) {
        final Map<String, Boolean> result = new LinkedHashMap<>();
        if (profile.expectedTraits() == null) return result;
        for (final String axis : NUMERIC_AXES) {
            final TraitPolarity polarity = profile.expectedTraits().get(axis);
            if (polarity == null) continue;
            final int score = scores.getOrDefault(axis, 3);
            result.put(axis, switch (polarity) {
                case HIGH -> score >= 4;
                case LOW -> score <= 2;
                case NEUTRAL -> true;
            });
        }
        return result;
    }
}

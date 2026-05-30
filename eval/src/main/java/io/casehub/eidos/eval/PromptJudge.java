package io.casehub.eidos.eval;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.request.json.*;
import io.casehub.eidos.api.AgentCapability;
import io.casehub.eidos.api.SystemPromptRenderer.RenderedPrompt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class PromptJudge {

    static final String SYSTEM_PROMPT = """
        You are evaluating the quality of an AI agent's system prompt.

        Given the agent's descriptor (JSON) and the rendered system prompt text,
        score each dimension from 0 to 5 and provide brief reasoning.

        Dimensions:
        - SECOND_PERSON (0-5): Uses "you"/"your" consistently. 5 = every sentence second person.
        - CONCISENESS (0-5): Every sentence carries unique information. No filler.
          5 = dense and efficient. 0 = heavily padded.
        - FACTUAL_FIDELITY (0-5): Nothing claimed absent from descriptor or context.
          5 = every claim grounded. 0 = significant hallucinations.
        - TONE (0-5): Reads as instructions to an AI agent, not documentation about one.
          5 = imperative, action-oriented. 0 = reads like a bio or README.

        Return JSON with keys SECOND_PERSON, CONCISENESS, FACTUAL_FIDELITY, TONE
        (each with "score" int 0-5 and "reasoning" string) and an "issues" string array.
        """;

    static final ResponseFormat RESPONSE_FORMAT = ResponseFormat.builder()
        .type(ResponseFormatType.JSON)
        .jsonSchema(JsonSchema.builder()
            .name("EvalJudgment")
            .rootElement(JsonObjectSchema.builder()
                .addProperty("SECOND_PERSON",    scoreSchema())
                .addProperty("CONCISENESS",      scoreSchema())
                .addProperty("FACTUAL_FIDELITY", scoreSchema())
                .addProperty("TONE",             scoreSchema())
                .addProperty("issues", JsonArraySchema.builder()
                    .description("Quality issues found.")
                    .items(JsonStringSchema.builder().build())
                    .build())
                .required("SECOND_PERSON", "CONCISENESS", "FACTUAL_FIDELITY", "TONE", "issues")
                .build())
            .build())
        .build();

    private static JsonObjectSchema scoreSchema() {
        return JsonObjectSchema.builder()
            .addIntegerProperty("score", "Score 0-5")
            .addStringProperty("reasoning", "Brief reasoning")
            .required("score", "reasoning")
            .build();
    }

    private final ChatModel judgeModel;
    private final ObjectMapper mapper;

    @Inject
    public PromptJudge(@Any final Instance<ChatModel> judgeModelInstance,
                       final ObjectMapper mapper) {
        if (!judgeModelInstance.isResolvable()) {
            throw new IllegalStateException(
                "Judge ChatModel not configured. Add a LangChain4j provider to eval/pom.xml " +
                "and configure credentials in application-eval.properties.");
        }
        this.judgeModel = judgeModelInstance.get();
        this.mapper = mapper;
    }

    /** Package-private constructor for unit tests — no CDI required. */
    PromptJudge(final ChatModel judgeModel, final ObjectMapper mapper) {
        this.judgeModel = judgeModel;
        this.mapper = mapper;
    }

    public EvalResult evaluate(final EvalCase evalCase, final RenderedPrompt rendered) {
        // 1. Programmatic completeness check (deterministic — not sent to LLM)
        final List<String> missing = evalCase.descriptor().capabilities().stream()
            .map(AgentCapability::name)
            .filter(n -> !rendered.content().contains(n))
            .toList();
        final boolean complete = missing.isEmpty();

        // 2. Judge LLM call — always made, regardless of completeness
        final ObjectNode userPayload = mapper.createObjectNode();
        try {
            userPayload.set("descriptor", mapper.valueToTree(evalCase.descriptor()));
            userPayload.put("rendered", rendered.content());
        } catch (final Exception e) {
            throw new IllegalStateException("Failed to build judge payload", e);
        }

        final Map<EvalDimension, EvalScore> scores;
        final List<String> issues;
        try {
            final var request = ChatRequest.builder()
                .messages(
                    SystemMessage.from(SYSTEM_PROMPT),
                    UserMessage.from(mapper.writeValueAsString(userPayload)))
                .responseFormat(RESPONSE_FORMAT)
                .build();
            final var response = judgeModel.chat(request);
            final var parsed = parseResponse(response.aiMessage().text());
            scores = parsed.scores();
            issues = parsed.issues();
        } catch (final Exception e) {
            throw new IllegalStateException("Judge LLM call failed — check judge model configuration", e);
        }

        final double overall = scores.values().stream()
            .mapToInt(EvalScore::score)
            .average()
            .orElse(0.0);

        return new EvalResult(evalCase, rendered, complete, missing, scores, overall, issues);
    }

    private record ParsedResponse(Map<EvalDimension, EvalScore> scores, List<String> issues) {}

    private ParsedResponse parseResponse(final String json) throws JsonProcessingException {
        final JsonNode root = mapper.readTree(json);
        final Map<EvalDimension, EvalScore> scores = new EnumMap<>(EvalDimension.class);

        // Iterate only known dimensions — skip "issues" and unknown keys
        for (final EvalDimension d : EvalDimension.values()) {
            final JsonNode dimNode = root.get(d.name());
            if (dimNode == null) throw new IllegalStateException("Judge response missing dimension: " + d.name());
            final JsonNode scoreNode = dimNode.get("score");
            final JsonNode reasoningNode = dimNode.get("reasoning");
            if (scoreNode == null || reasoningNode == null) {
                throw new IllegalStateException(
                    "Judge response for " + d.name() + " missing 'score' or 'reasoning'");
            }
            scores.put(d, new EvalScore(scoreNode.asInt(), reasoningNode.asText()));
        }

        final List<String> issues = new ArrayList<>();
        final JsonNode issuesNode = root.get("issues");
        if (issuesNode != null && issuesNode.isArray()) {
            issuesNode.forEach(n -> issues.add(n.asText()));
        }
        return new ParsedResponse(scores, issues);
    }
}

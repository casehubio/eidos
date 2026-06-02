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
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import io.casehub.eidos.api.SystemPromptRenderer.RenderedPrompt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class PromptJudge {

    static final String MARKDOWN_SYSTEM_PROMPT = """
        You are evaluating the quality of an AI agent's system prompt in markdown format.

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

    static final String PROSE_SYSTEM_PROMPT = """
        You are evaluating the quality of an AI agent's system prompt in dense prose format.

        Given the agent's descriptor (JSON) and the rendered system prompt text,
        score each dimension from 0 to 5 and provide brief reasoning.

        Dimensions:
        - SECOND_PERSON (0-5): Uses "you"/"your" consistently. 5 = every sentence second person.
        - CONCISENESS (0-5): Dense prose required — no markdown headers or bullets.
          5 = dense, efficient prose with no markdown. 2 or less if any markdown header (#)
          or bullet point (- or *) is found anywhere in the text.
        - FACTUAL_FIDELITY (0-5): Nothing claimed absent from descriptor or context.
          5 = every claim grounded. 0 = significant hallucinations.
        - TONE (0-5): Reads as instructions to an AI agent, not documentation about one.
          5 = imperative, action-oriented. 0 = reads like a bio or README.

        Return JSON with keys SECOND_PERSON, CONCISENESS, FACTUAL_FIDELITY, TONE
        (each with "score" int 0-5 and "reasoning" string) and an "issues" string array.
        """;

    static final String A2A_SYSTEM_PROMPT = """
        You are evaluating the quality of an AI agent's A2A (agent-to-agent) identity card in JSON format.

        Given the agent's descriptor (JSON) and the rendered A2A card (JSON),
        score each dimension from 0 to 5 and provide brief reasoning.

        Dimensions:
        - COMPLETENESS (0-5): Every declared capability has a non-empty "description" field
          that meaningfully describes what the agent can do with that capability.
          5 = all capabilities have clear, accurate descriptions. 0 = no descriptions.
        - FACTUAL_FIDELITY (0-5): Nothing in the card is absent from the descriptor.
          No hallucinated capabilities, no fabricated names or versions.
          5 = every field grounded in descriptor data. 0 = significant hallucinations.

        Return JSON with keys COMPLETENESS, FACTUAL_FIDELITY
        (each with "score" int 0-5 and "reasoning" string) and an "issues" string array.
        """;

    static final ResponseFormat STANDARD_JUDGE_RESPONSE_FORMAT = ResponseFormat.builder()
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

    static final ResponseFormat A2A_JUDGE_RESPONSE_FORMAT = ResponseFormat.builder()
        .type(ResponseFormatType.JSON)
        .jsonSchema(JsonSchema.builder()
            .name("A2AEvalJudgment")
            .rootElement(JsonObjectSchema.builder()
                .addProperty("COMPLETENESS",     scoreSchema())
                .addProperty("FACTUAL_FIDELITY", scoreSchema())
                .addProperty("issues", JsonArraySchema.builder()
                    .description("Quality issues found.")
                    .items(JsonStringSchema.builder().build())
                    .build())
                .required("COMPLETENESS", "FACTUAL_FIDELITY", "issues")
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
        final RenderFormat format = evalCase.context().format();
        final Set<EvalDimension> applicable = EvalDimension.applicableFor(format);

        // 1. Format-aware completeness check (deterministic — not sent to LLM)
        final List<String> missing = computeMissingCapabilities(evalCase, rendered);
        final boolean complete = missing.isEmpty();

        // 2. Build judge payload
        final String userPayloadJson;
        try {
            final ObjectNode userPayload = mapper.createObjectNode();
            userPayload.set("descriptor", mapper.valueToTree(evalCase.descriptor()));
            userPayload.put("rendered", rendered.content());
            userPayloadJson = mapper.writeValueAsString(userPayload);
        } catch (final Exception e) {
            throw new IllegalStateException("Failed to build judge payload", e);
        }

        // 3. Select system prompt and schema based on format
        final String systemPrompt = switch (format) {
            case MARKDOWN  -> MARKDOWN_SYSTEM_PROMPT;
            case PROSE     -> PROSE_SYSTEM_PROMPT;
            case A2A_CARD  -> A2A_SYSTEM_PROMPT;
        };
        final ResponseFormat responseFormat = switch (format) {
            case MARKDOWN, PROSE -> STANDARD_JUDGE_RESPONSE_FORMAT;
            case A2A_CARD        -> A2A_JUDGE_RESPONSE_FORMAT;
        };

        // 4. Call judge LLM
        final Map<EvalDimension, EvalScore> scores;
        final List<String> issues;
        try {
            final var request = ChatRequest.builder()
                .messages(
                    SystemMessage.from(systemPrompt),
                    UserMessage.from(userPayloadJson))
                .responseFormat(responseFormat)
                .build();
            final var response = judgeModel.chat(request);
            final var parsed = parseResponse(response.aiMessage().text(), applicable);
            scores = parsed.scores();
            issues = parsed.issues();
        } catch (final MalformedJudgeResponseException e) {
            throw e;
        } catch (final Exception e) {
            throw new IllegalStateException("Judge LLM call failed — check judge model configuration", e);
        }

        final double overall = scores.values().stream()
            .mapToInt(EvalScore::score)
            .average()
            .orElse(0.0);

        return new EvalResult(evalCase, rendered, complete, missing, scores, overall, issues);
    }

    private List<String> computeMissingCapabilities(final EvalCase evalCase,
                                                     final RenderedPrompt rendered) {
        if (evalCase.context().format() == RenderFormat.A2A_CARD) {
            return computeA2aMissingDescriptions(evalCase, rendered.content());
        }
        return evalCase.descriptor().capabilities().stream()
            .map(AgentCapability::name)
            .filter(n -> !rendered.content().contains(n))
            .toList();
    }

    private List<String> computeA2aMissingDescriptions(final EvalCase evalCase,
                                                        final String json) {
        try {
            final JsonNode root = mapper.readTree(json);
            final JsonNode caps = root.get("capabilities");
            if (caps == null || !caps.isArray()) {
                return evalCase.descriptor().capabilities().isEmpty()
                    ? List.of()
                    : evalCase.descriptor().capabilities().stream()
                        .map(AgentCapability::name).toList();
            }
            final Map<String, String> descriptions = new HashMap<>();
            caps.forEach(cap -> {
                final JsonNode name = cap.get("name");
                final JsonNode desc = cap.get("description");
                if (name != null) {
                    descriptions.put(name.asText(), desc != null ? desc.asText() : "");
                }
            });
            return evalCase.descriptor().capabilities().stream()
                .map(AgentCapability::name)
                .filter(n -> {
                    final String desc = descriptions.get(n);
                    return desc == null || desc.isBlank();
                })
                .toList();
        } catch (final Exception e) {
            throw new IllegalStateException("Failed to parse A2A card JSON for completeness check", e);
        }
    }

    private record ParsedResponse(Map<EvalDimension, EvalScore> scores, List<String> issues) {}

    private ParsedResponse parseResponse(final String json,
                                          final Set<EvalDimension> applicable) {
        final JsonNode root;
        try {
            root = mapper.readTree(json);
        } catch (final JsonProcessingException e) {
            throw new MalformedJudgeResponseException("Judge returned non-JSON response: " + e.getMessage());
        }
        final Map<EvalDimension, EvalScore> scores = new EnumMap<>(EvalDimension.class);

        // Iterate applicable dimensions only — extra keys in response are ignored
        for (final EvalDimension d : applicable) {
            final JsonNode dimNode = root.get(d.name());
            if (dimNode == null) {
                throw new MalformedJudgeResponseException("Judge response missing dimension: " + d.name());
            }
            final JsonNode scoreNode = dimNode.get("score");
            final JsonNode reasoningNode = dimNode.get("reasoning");
            if (scoreNode == null || reasoningNode == null) {
                throw new MalformedJudgeResponseException(
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

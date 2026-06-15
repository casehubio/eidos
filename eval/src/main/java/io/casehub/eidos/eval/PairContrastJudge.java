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

import java.util.Map;

@ApplicationScoped
public class PairContrastJudge {

    static final String SYSTEM_TEMPLATE = """
        You are comparing two AI agent system prompts on a specific personality axis.

        Axis: [%s]
        Prompt A: %s
        Prompt B: %s

        Identify which prompt expresses the axis more strongly, and score how starkly
        different they are.

        Effect size rubric (1–5):
        - 5 = unmistakably different; a naive reader could identify which is which
        - 3 = distinguishable if you are looking for it
        - 1 = practically indistinguishable on this axis

        Return JSON: { "higher": "A" | "B", "effectSize": int, "reasoning": string }
        """;

    static final ResponseFormat RESPONSE_FORMAT = ResponseFormat.builder()
        .type(ResponseFormatType.JSON)
        .jsonSchema(JsonSchema.builder()
            .name("PairContrast")
            .rootElement(JsonObjectSchema.builder()
                .addStringProperty("higher", "A or B")
                .addIntegerProperty("effectSize", "1–5")
                .addStringProperty("reasoning", "Explanation")
                .required("higher", "effectSize", "reasoning")
                .build())
            .build())
        .build();

    private final ChatModel model;
    private final ObjectMapper mapper;

    @Inject
    public PairContrastJudge(@Any final Instance<ChatModel> models, final ObjectMapper mapper) {
        if (!models.isResolvable()) throw new IllegalStateException("ChatModel not configured.");
        this.model = models.get();
        this.mapper = mapper;
    }

    PairContrastJudge(final ChatModel model, final ObjectMapper mapper) {
        this.model = model;
        this.mapper = mapper;
    }

    public PairContrastResult evaluate(final VariantPair pair,
                                        final RenderFormat format,
                                        final Map<ProfiledEvalCase, RenderedPrompt> renders) {
        final String higherText = findRender(renders, pair.higher(), format);
        final String lowerText = findRender(renders, pair.lower(), format);
        final String systemPrompt = String.format(
            SYSTEM_TEMPLATE, pair.primaryAxis().description(), higherText, lowerText);
        try {
            final var request = ChatRequest.builder()
                .messages(
                    SystemMessage.from(systemPrompt),
                    UserMessage.from("Compare the two prompts."))
                .responseFormat(RESPONSE_FORMAT)
                .build();
            PairContrastResult result;
            try {
                result = parse(pair, format, model.chat(request).aiMessage().text());
            } catch (final MalformedJudgeResponseException first) {
                System.err.printf("[WARN] PairContrastJudge non-JSON response, retrying (%s)%n", first.getMessage());
                result = parse(pair, format, model.chat(request).aiMessage().text());
            }
            return result;
        } catch (final MalformedJudgeResponseException e) {
            throw e;
        } catch (final Exception e) {
            throw new IllegalStateException("PairContrastJudge LLM call failed", e);
        }
    }

    private String findRender(final Map<ProfiledEvalCase, RenderedPrompt> renders,
                               final String profileSlug,
                               final RenderFormat format) {
        return renders.entrySet().stream()
            .filter(e -> e.getKey().profile().name().equals(profileSlug)
                && e.getKey().context().format() == format)
            .map(e -> e.getValue().content())
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "No render found for profile: " + profileSlug + ", format: " + format));
    }

    private PairContrastResult parse(final VariantPair pair,
                                      final RenderFormat format,
                                      final String json) throws JsonProcessingException {
        final JsonNode root = mapper.readTree(PromptJudge.extractJson(json));
        final JsonNode higher = root.get("higher");
        final JsonNode effectSize = root.get("effectSize");
        final JsonNode reasoning = root.get("reasoning");
        if (higher == null || effectSize == null || reasoning == null)
            throw new MalformedJudgeResponseException("PairContrastJudge response missing fields");
        final boolean correct = "A".equals(higher.asText());
        return new PairContrastResult(
            pair.higher(), pair.lower(), pair.primaryAxis(),
            format, correct, effectSize.asInt(), reasoning.asText());
    }
}

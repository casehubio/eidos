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
import io.casehub.eidos.api.SystemPromptRenderer.RenderedPrompt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class ProximityJudge {

    static final String SYSTEM_PROMPT = """
        You are evaluating how faithfully a machine-rendered system prompt captures
        the identity expressed in a human-authored system prompt.

        The human-authored prompt is the ground truth. The machine-rendered prompt was
        derived by structuring the human prompt into an AgentDescriptor, then rendering
        that descriptor back into a system prompt.

        Score 0–5:
        - 5: Conveys the same role, constraints, and operational style.
        - 4: Minor gaps — one or two concepts softened or absent.
        - 3: Core role present but significant style, constraints, or domain context missing.
        - 2: Role recognisable but rendering loses enough to change agent behaviour.
        - 1: Superficial match — same domain, fundamentally different character.
        - 0: Identity mismatch.

        Scoring guidance: Treat additions from the descriptor (not in the original prose)
        as neutral unless they contradict the original. Treat as a gap only content absent
        from BOTH the original prose AND the descriptor.

        Return JSON: { "score": int, "reasoning": string, "gaps": string[] }
        """;

    static final ResponseFormat RESPONSE_FORMAT = ResponseFormat.builder()
        .type(ResponseFormatType.JSON)
        .jsonSchema(JsonSchema.builder()
            .name("ProximityJudgment")
            .rootElement(JsonObjectSchema.builder()
                .addIntegerProperty("score", "Score 0–5")
                .addStringProperty("reasoning", "Explanation")
                .addProperty("gaps", JsonArraySchema.builder()
                    .items(JsonStringSchema.builder().build()).build())
                .required("score", "reasoning", "gaps")
                .build())
            .build())
        .build();

    private final ChatModel model;
    private final ObjectMapper mapper;

    @Inject
    public ProximityJudge(@Any final Instance<ChatModel> models, final ObjectMapper mapper) {
        if (!models.isResolvable()) throw new IllegalStateException(
            "Judge ChatModel not configured.");
        this.model = models.get();
        this.mapper = mapper;
    }

    /** Package-private constructor for unit tests — no CDI required. */
    ProximityJudge(final ChatModel model, final ObjectMapper mapper) {
        this.model = model;
        this.mapper = mapper;
    }

    public ProximityResult evaluate(final ProfiledEvalCase evalCase, final RenderedPrompt rendered) {
        final ObjectNode payload = mapper.createObjectNode();
        payload.put("originalProse", evalCase.profile().originalProse());
        payload.put("rendered", rendered.content());
        try {
            final var request = ChatRequest.builder()
                .messages(
                    SystemMessage.from(SYSTEM_PROMPT),
                    UserMessage.from(mapper.writeValueAsString(payload)))
                .responseFormat(RESPONSE_FORMAT)
                .build();
            final var response = model.chat(request);
            return parse(evalCase, response.aiMessage().text());
        } catch (final MalformedJudgeResponseException e) {
            throw e;
        } catch (final Exception e) {
            throw new IllegalStateException("ProximityJudge LLM call failed", e);
        }
    }

    private ProximityResult parse(final ProfiledEvalCase evalCase, final String json)
            throws JsonProcessingException {
        final JsonNode root = mapper.readTree(json);
        final JsonNode score = root.get("score");
        final JsonNode reasoning = root.get("reasoning");
        if (score == null || reasoning == null)
            throw new MalformedJudgeResponseException(
                "ProximityJudge response missing score or reasoning");
        final List<String> gaps = new ArrayList<>();
        final JsonNode gapsNode = root.get("gaps");
        if (gapsNode != null && gapsNode.isArray()) gapsNode.forEach(n -> gaps.add(n.asText()));
        return new ProximityResult(evalCase, score.asInt(), reasoning.asText(), gaps);
    }
}

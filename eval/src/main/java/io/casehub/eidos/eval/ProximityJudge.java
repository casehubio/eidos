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
import io.casehub.eidos.api.AgentDisposition;
import io.casehub.eidos.api.DispositionAxis;
import io.casehub.eidos.api.SystemPromptRenderer.RenderedPrompt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class ProximityJudge {

    static final String SYSTEM_PROMPT = """
        You are evaluating whether a rendered AI agent system prompt correctly and completely \
        expresses the agent's disposition axes as declared in the descriptor.

        For each axis present in the disposition object, check whether the render conveys \
        that axis value.

        Score 0–5:
        - 5: All axes clearly and correctly expressed.
        - 4: Minor omission or softening of one axis.
        - 3: Partial coverage — some axes present, others missing.
        - 2: Significant axes missing from the render.
        - 1: Most axes missing or contradicted by the render.
        - 0: Disposition entirely absent from the render.

        If no disposition object is provided in the payload, the descriptor has no disposition \
        axes — return score: 5, reasoning: "No disposition axes declared", gaps: [].

        Return JSON: { "score": int, "reasoning": string, "gaps": string[] }
        """;

    private static final Logger log = Logger.getLogger(ProximityJudge.class);

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

        // Build disposition node using only non-null axes.
        // DO NOT use mapper.valueToTree(disposition) — it includes null fields as JSON null,
        // which causes the judge to attempt evaluation of absent axes and score incorrectly.
        final AgentDisposition disp = evalCase.descriptor().disposition();
        if (disp != null) {
            final ObjectNode dispNode = mapper.createObjectNode();
            for (final DispositionAxis axis : DispositionAxis.values()) {
                disp.get(axis).ifPresent(raw -> dispNode.put(axis.jsonKey(), raw));
            }
            dispNode.put("canDelegate", disp.delegation());
            payload.set("disposition", dispNode);
        }
        // If disp is null, no "disposition" key is added — judge returns score 5 per SYSTEM_PROMPT.

        payload.put("rendered", rendered.content());

        try {
            final var request = ChatRequest.builder()
                .messages(
                    SystemMessage.from(SYSTEM_PROMPT),
                    UserMessage.from(mapper.writeValueAsString(payload)))
                .responseFormat(RESPONSE_FORMAT)
                .build();
            ProximityResult result;
            try {
                result = parse(evalCase, model.chat(request).aiMessage().text());
            } catch (final MalformedJudgeResponseException | JsonProcessingException first) {
                log.warnf("ProximityJudge non-JSON response, retrying (%s)", first.getMessage());
                try {
                    result = parse(evalCase, model.chat(request).aiMessage().text());
                } catch (final JsonProcessingException retry) {
                    throw new MalformedJudgeResponseException(
                        "ProximityJudge returned non-JSON on retry: " + retry.getMessage());
                }
            }
            return result;
        } catch (final MalformedJudgeResponseException e) {
            throw e;
        } catch (final Exception e) {
            throw new IllegalStateException("ProximityJudge LLM call failed", e);
        }
    }

    private ProximityResult parse(final ProfiledEvalCase evalCase, final String json)
            throws JsonProcessingException {
        final JsonNode root = mapper.readTree(PromptJudge.extractJson(json));
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

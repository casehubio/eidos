package io.casehub.eidos.eval;

import com.fasterxml.jackson.core.JsonProcessingException;
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

@ApplicationScoped
public class BehavioralJudge {

    static final String SYSTEM_TEMPLATE = """
        You are comparing two AI agent responses to the same question.

        Axis being assessed: [%s]

        Question: %s

        Response A: %s

        Response B: %s

        Which response expresses the axis value more strongly?

        Effect size (1-5):
        - 5 = unmistakably different; a reader could identify which without knowing the axis
        - 3 = distinguishable if you are looking for it
        - 1 = practically indistinguishable on this axis

        Return JSON: { "higher": "A" | "B", "effectSize": int, "reasoning": string }
        """;

    private final ChatModel judgeModel;
    private final ObjectMapper mapper;

    @Inject
    public BehavioralJudge(@Any final Instance<ChatModel> models, final ObjectMapper mapper) {
        if (!models.isResolvable()) throw new IllegalStateException("ChatModel not configured.");
        this.judgeModel = models.get();
        this.mapper = mapper;
    }

    /** Package-private constructor for unit tests — no CDI required. */
    BehavioralJudge(final ChatModel judgeModel, final ObjectMapper mapper) {
        this.judgeModel = judgeModel;
        this.mapper = mapper;
    }

    /**
     * Evaluates whether a blind judge can identify which response (A=higher, B=lower)
     * expresses the pair's primary axis more strongly.
     */
    public BehavioralPairResult evaluate(final VariantPair pair, final String question,
                                          final String higherResponse, final String lowerResponse) {
        final String systemPrompt = String.format(
            SYSTEM_TEMPLATE,
            pair.primaryAxis().description(),
            question,
            higherResponse,
            lowerResponse);
        try {
            final var request = ChatRequest.builder()
                .messages(
                    SystemMessage.from(systemPrompt),
                    UserMessage.from("Compare the two responses."))
                .build();
            final var response = judgeModel.chat(request);
            return parse(pair, question, higherResponse, lowerResponse, response.aiMessage().text());
        } catch (final MalformedJudgeResponseException e) {
            throw e;
        } catch (final Exception e) {
            throw new IllegalStateException("BehavioralJudge LLM call failed", e);
        }
    }

    private BehavioralPairResult parse(final VariantPair pair, final String question,
                                        final String higherResponse, final String lowerResponse,
                                        final String json) throws JsonProcessingException {
        final JsonNode root = mapper.readTree(json);
        final JsonNode higher = root.get("higher");
        final JsonNode effectSize = root.get("effectSize");
        final JsonNode reasoning = root.get("reasoning");
        if (higher == null || effectSize == null || reasoning == null)
            throw new MalformedJudgeResponseException("BehavioralJudge response missing fields");
        final boolean correct = "A".equals(higher.asText());
        return new BehavioralPairResult(
            pair, question, higherResponse, lowerResponse,
            correct, effectSize.asInt(), reasoning.asText());
    }
}

package io.casehub.eidos.runtime.renderer;

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
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import org.jboss.logging.Logger;

import java.util.Optional;

class SemanticEnrichmentStep {

    private static final Logger log = Logger.getLogger(SemanticEnrichmentStep.class);

    // Declaration order is load-order: RESPONSE_FORMAT is self-contained, no field dependency.
    static final ResponseFormat RESPONSE_FORMAT = ResponseFormat.builder()
            .type(ResponseFormatType.JSON)
            .jsonSchema(JsonSchema.builder()
                    .name("SemanticEnrichment")
                    .rootElement(JsonObjectSchema.builder()
                            .addStringProperty("identityNarrative",
                                    "Who this agent is — name, model, version context. Second person.")
                            .addStringProperty("roleNarrative",
                                    "The agent's role and purpose. Second person.")
                            .addStringProperty("capabilityNarrative",
                                    "What the agent can do, including domain confidence. Second person.")
                            .addStringProperty("dispositionNarrative",
                                    "How the agent operates. Empty string if no disposition data.")
                            .addStringProperty("constraintNarrative",
                                    "Data handling obligations. Empty string if none.")
                            .addStringProperty("goalNarrative",
                                    "Current task and objectives. Empty string if no goal.")
                            .required("identityNarrative", "roleNarrative", "capabilityNarrative",
                                    "dispositionNarrative", "constraintNarrative", "goalNarrative")
                            .build())
                    .build())
            .build();

    private final ObjectMapper mapper;

    SemanticEnrichmentStep(final ObjectMapper mapper) {
        this.mapper = mapper;
    }

    Optional<SemanticEnrichment> enrich(final ChatModel llm, final ObjectNode payload) {
        try {
            final ChatRequest request = ChatRequest.builder()
                    .messages(
                            SystemMessage.from(ClaudeMarkdownRenderer.PROMPT_TEMPLATE),
                            UserMessage.from(mapper.writeValueAsString(payload))
                    )
                    .responseFormat(RESPONSE_FORMAT)
                    .build();

            final var response = llm.chat(request);
            return Optional.of(parse(response.aiMessage().text()));

        } catch (final Exception e) {
            log.warn("Semantic enrichment failed (" + e.getMessage()
                    + "), falling back to structural rendering");
            return Optional.empty();
        }
    }

    private SemanticEnrichment parse(final String json) throws JsonProcessingException {
        final JsonNode node = mapper.readTree(json);
        return new SemanticEnrichment(
                node.get("identityNarrative").asText(),
                node.get("roleNarrative").asText(),
                node.get("capabilityNarrative").asText(),
                optional(node, "dispositionNarrative"),
                optional(node, "constraintNarrative"),
                optional(node, "goalNarrative")
        );
    }

    private static Optional<String> optional(final JsonNode node, final String field) {
        final JsonNode n = node.get(field);
        if (n == null || n.isNull()) return Optional.empty();
        final String v = n.asText("").strip();
        return v.isEmpty() ? Optional.empty() : Optional.of(v);
    }
}

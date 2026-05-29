package io.casehub.eidos.runtime.renderer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import org.jboss.logging.Logger;

import java.util.Optional;

class SemanticEnrichmentStep {

    private static final Logger log = Logger.getLogger(SemanticEnrichmentStep.class);

    private final ObjectMapper mapper;

    SemanticEnrichmentStep(final ObjectMapper mapper) {
        this.mapper = mapper;
    }

    Optional<SemanticEnrichment> enrich(final ChatModel llm, final ObjectNode payload) {
        try {
            final ChatRequest request = ChatRequest.builder()
                    .messages(
                            SystemMessage.from(EidosRenderPipeline.PROMPT_TEMPLATE),
                            UserMessage.from(mapper.writeValueAsString(payload))
                    )
                    .responseFormat(EidosRenderPipeline.RESPONSE_FORMAT)
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

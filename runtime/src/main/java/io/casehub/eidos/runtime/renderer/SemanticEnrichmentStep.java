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
            try {
                return Optional.of(parse(llm.chat(request).aiMessage().text()));
            } catch (final JsonProcessingException first) {
                log.warnf("Enrichment: non-JSON response, retrying (%s)", first.getMessage());
                return Optional.of(parse(llm.chat(request).aiMessage().text()));
            }
        } catch (final Exception e) {
            log.warnf("Semantic enrichment failed (%s), falling back to structural", e.getMessage());
            return Optional.empty();
        }
    }

    private SemanticEnrichment parse(final String json) throws JsonProcessingException {
        final JsonNode node = mapper.readTree(JsonExtractionUtil.extractJson(json));
        return new SemanticEnrichment(
                SemanticEnrichment.parseOptional(node, "dispositionNarrative"),
                SemanticEnrichment.parseOptional(node, "goalNarrative")
        );
    }
}

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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class A2ASemanticEnrichmentStep {

    private static final Logger log = Logger.getLogger(A2ASemanticEnrichmentStep.class);

    private final ObjectMapper mapper;

    A2ASemanticEnrichmentStep(final ObjectMapper mapper) {
        this.mapper = mapper;
    }

    Optional<A2AEnrichment> enrich(final ChatModel llm, final ObjectNode descriptorNode) {
        try {
            final ChatRequest request = ChatRequest.builder()
                    .messages(
                            SystemMessage.from(EidosRenderPipeline.A2A_PROMPT_TEMPLATE),
                            UserMessage.from(mapper.writeValueAsString(descriptorNode))
                    )
                    .responseFormat(EidosRenderPipeline.A2A_RESPONSE_FORMAT)
                    .build();

            final var response = llm.chat(request);
            return Optional.of(parse(response.aiMessage().text()));

        } catch (final Exception e) {
            log.warnf("A2A enrichment failed (%s), falling back to structural A2A rendering",
                    e.getMessage());
            return Optional.empty();
        }
    }

    private A2AEnrichment parse(final String json) throws JsonProcessingException {
        final JsonNode node = mapper.readTree(SemanticEnrichmentStep.stripCodeFences(json));
        final JsonNode narrativesNode = node.get("capabilityNarratives");
        if (narrativesNode == null || !narrativesNode.isArray()) {
            return new A2AEnrichment(List.of());
        }
        final List<A2AEnrichment.CapabilityNarrative> narratives = new ArrayList<>();
        for (final JsonNode item : narrativesNode) {
            final String name = item.path("name").asText(null);
            final String description = item.path("description").asText(null);
            if (name != null && !name.isBlank() && description != null && !description.isBlank()) {
                narratives.add(new A2AEnrichment.CapabilityNarrative(name, description));
            }
        }
        return new A2AEnrichment(List.copyOf(narratives));
    }
}

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
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import org.jboss.logging.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class A2ASemanticEnrichmentStep {

    private static final Logger log = Logger.getLogger(A2ASemanticEnrichmentStep.class);

    static final String A2A_PROMPT_TEMPLATE = """
            You are writing per-capability descriptions for an AI agent's A2A (agent-to-agent) card.

            Given the agent's capabilities in JSON, produce a JSON object with one prose description
            per declared capability. Write in second person, addressing the agent directly.

            RULES:
            - Copy the capability name exactly as given — do not paraphrase or change capitalisation.
            - Each description is 1-2 sentences. Second person ("You can...").
            - Plain prose. No markdown, no bullet points.
            - Return ONLY the JSON object. No explanation, no preamble, no code fences.
            - If no capabilities are declared, return {"capabilityNarratives": []}.""";

    static final ResponseFormat A2A_RESPONSE_FORMAT = ResponseFormat.builder()
            .type(ResponseFormatType.JSON)
            .jsonSchema(JsonSchema.builder()
                    .name("A2AEnrichment")
                    .rootElement(JsonObjectSchema.builder()
                            .addProperty("capabilityNarratives", JsonArraySchema.builder()
                                    .description("One entry per declared capability. Empty array [] if none.")
                                    .items(JsonObjectSchema.builder()
                                            .addStringProperty("name",
                                                    "Capability name — must match exactly as given.")
                                            .addStringProperty("description",
                                                    "1-2 sentences, second person, what this agent can do with this capability.")
                                            .required("name", "description")
                                            .build())
                                    .build())
                            .required("capabilityNarratives")
                            .build())
                    .build())
            .build();

    private final ObjectMapper mapper;

    A2ASemanticEnrichmentStep(final ObjectMapper mapper) {
        this.mapper = mapper;
    }

    Optional<A2AEnrichment> enrich(final ChatModel llm, final ObjectNode descriptorNode) {
        try {
            final ChatRequest request = ChatRequest.builder()
                    .messages(
                            SystemMessage.from(A2A_PROMPT_TEMPLATE),
                            UserMessage.from(mapper.writeValueAsString(descriptorNode))
                    )
                    .responseFormat(A2A_RESPONSE_FORMAT)
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
        final JsonNode node = mapper.readTree(json);
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

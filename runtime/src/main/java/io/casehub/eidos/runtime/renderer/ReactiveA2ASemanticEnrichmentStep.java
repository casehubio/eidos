package io.casehub.eidos.runtime.renderer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import io.smallrye.mutiny.Uni;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Async A2A enrichment: wraps StreamingChatModel.chat() in a CompletableFuture→Uni bridge.
 * No thread is held during LLM inference. Always completes Optional.empty() on failure.
 */
class ReactiveA2ASemanticEnrichmentStep {

    private static final Logger log = Logger.getLogger(ReactiveA2ASemanticEnrichmentStep.class);

    private final ObjectMapper mapper;

    ReactiveA2ASemanticEnrichmentStep(final ObjectMapper mapper) {
        this.mapper = mapper;
    }

    Uni<Optional<A2AEnrichment>> enrich(final StreamingChatModel llm, final ObjectNode payload) {
        final ChatRequest request;
        try {
            request = ChatRequest.builder()
                    .messages(
                            SystemMessage.from(EidosRenderPipeline.A2A_PROMPT_TEMPLATE),
                            UserMessage.from(mapper.writeValueAsString(payload))
                    )
                    .responseFormat(EidosRenderPipeline.A2A_RESPONSE_FORMAT)
                    .build();
        } catch (final Exception e) {
            log.warn("Reactive A2A enrichment: request build failed (" + e.getMessage() + "), falling back");
            return Uni.createFrom().item(Optional.empty());
        }

        final CompletableFuture<Optional<A2AEnrichment>> future = new CompletableFuture<>();
        llm.chat(request, new StreamingChatResponseHandler() {
            @Override
            public void onCompleteResponse(final ChatResponse response) {
                future.complete(parseOrEmpty(response.aiMessage().text()));
            }

            @Override
            public void onError(final Throwable error) {
                future.completeExceptionally(error);
            }
        });

        return Uni.createFrom().completionStage(
                        future.orTimeout(EidosRenderPipeline.STREAMING_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                .onFailure().recoverWithItem(e -> {
                    log.warn("Reactive A2A semantic enrichment failed (" + e.getMessage()
                            + "), falling back to structural A2A rendering");
                    return Optional.empty();
                });
    }

    private Optional<A2AEnrichment> parseOrEmpty(final String json) {
        try {
            final JsonNode node = mapper.readTree(json);
            final JsonNode narrativesNode = node.get("capabilityNarratives");
            if (narrativesNode == null || !narrativesNode.isArray()) {
                return Optional.of(new A2AEnrichment(List.of()));
            }
            final List<A2AEnrichment.CapabilityNarrative> narratives = new ArrayList<>();
            for (final JsonNode item : narrativesNode) {
                final String name = item.path("name").asText(null);
                final String description = item.path("description").asText(null);
                if (name != null && !name.isBlank() && description != null && !description.isBlank()) {
                    narratives.add(new A2AEnrichment.CapabilityNarrative(name, description));
                }
            }
            return Optional.of(new A2AEnrichment(List.copyOf(narratives)));
        } catch (final Exception e) {
            log.warn("Reactive A2A enrichment: parse failed (" + e.getMessage() + "), falling back");
            return Optional.empty();
        }
    }
}

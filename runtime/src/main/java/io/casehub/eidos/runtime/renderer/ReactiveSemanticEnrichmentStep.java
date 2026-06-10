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

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Async enrichment: wraps StreamingChatModel.chat() in a CompletableFuture→Uni bridge.
 * No thread is held during LLM inference. Always completes Optional.empty() on failure.
 */
class ReactiveSemanticEnrichmentStep {

    private static final Logger log = Logger.getLogger(ReactiveSemanticEnrichmentStep.class);

    private final ObjectMapper mapper;

    ReactiveSemanticEnrichmentStep(final ObjectMapper mapper) {
        this.mapper = mapper;
    }

    Uni<Optional<SemanticEnrichment>> enrich(final StreamingChatModel llm, final ObjectNode payload) {
        final ChatRequest request;
        try {
            request = ChatRequest.builder()
                    .messages(
                            SystemMessage.from(EidosRenderPipeline.PROMPT_TEMPLATE),
                            UserMessage.from(mapper.writeValueAsString(payload))
                    )
                    .responseFormat(EidosRenderPipeline.RESPONSE_FORMAT)
                    .build();
        } catch (final Exception e) {
            log.warn("Reactive enrichment: request build failed (" + e.getMessage() + "), falling back");
            return Uni.createFrom().item(Optional.empty());
        }

        final CompletableFuture<Optional<SemanticEnrichment>> future = new CompletableFuture<>();
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
                    log.warn("Reactive semantic enrichment failed (" + e.getMessage()
                            + "), falling back to structural rendering");
                    return Optional.empty();
                });
    }

    private Optional<SemanticEnrichment> parseOrEmpty(final String json) {
        try {
            final JsonNode node = mapper.readTree(SemanticEnrichmentStep.stripCodeFences(json));
            return Optional.of(new SemanticEnrichment(
                    node.get("identityNarrative").asText(),
                    node.get("roleNarrative").asText(),
                    node.get("capabilityNarrative").asText(),
                    optional(node, "dispositionNarrative"),
                    optional(node, "constraintNarrative"),
                    optional(node, "goalNarrative")
            ));
        } catch (final Exception e) {
            log.warn("Reactive enrichment: parse failed (" + e.getMessage() + "), falling back");
            return Optional.empty();
        }
    }

    private static Optional<String> optional(final JsonNode node, final String field) {
        final JsonNode n = node.get(field);
        if (n == null || n.isNull()) return Optional.empty();
        final String v = n.asText("").strip();
        return v.isEmpty() ? Optional.empty() : Optional.of(v);
    }
}

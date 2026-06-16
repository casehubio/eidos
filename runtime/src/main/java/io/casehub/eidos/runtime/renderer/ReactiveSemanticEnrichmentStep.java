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
            log.warnf("Reactive enrichment: request build failed (%s), falling back", e.getMessage());
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
                    log.warnf("Reactive semantic enrichment failed (%s), falling back to structural rendering",
                            e.getMessage());
                    return Optional.empty();
                });
    }

    private Optional<SemanticEnrichment> parseOrEmpty(final String json) {
        try {
            final JsonNode node = mapper.readTree(JsonExtractionUtil.extractJson(json));
            return Optional.of(new SemanticEnrichment(
                    SemanticEnrichment.parseOptional(node, "dispositionNarrative"),
                    SemanticEnrichment.parseOptional(node, "goalNarrative")
            ));
        } catch (final Exception e) {
            log.warnf("Reactive enrichment: parse failed (%s), falling back", e.getMessage());
            return Optional.empty();
        }
    }
}

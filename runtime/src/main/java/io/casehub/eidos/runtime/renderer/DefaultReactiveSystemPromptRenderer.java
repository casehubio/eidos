package io.casehub.eidos.runtime.renderer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentPromptContext;
import io.casehub.eidos.api.ReactiveSystemPromptRenderer;
import io.casehub.eidos.api.SystemPromptRenderer;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import io.casehub.eidos.api.SystemPromptRenderer.RenderedPrompt;
import io.quarkus.arc.DefaultBean;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import dev.langchain4j.model.chat.StreamingChatModel;

import java.util.Optional;

@DefaultBean
@ApplicationScoped
public class DefaultReactiveSystemPromptRenderer implements ReactiveSystemPromptRenderer {

    // StreamingChatModel must be @ApplicationScoped (or broader). A @Dependent-scoped
    // StreamingChatModel obtained via Instance.get() would leak. Quarkus LangChain4j
    // always registers StreamingChatModel as @ApplicationScoped, so this is safe.
    private final StreamingChatModel streamingLlm;
    private final SystemPromptRenderer blockingDelegate;
    private final EidosRenderPipeline pipeline;
    private final ReactiveSemanticEnrichmentStep reactiveEnrichStep;
    private final ReactiveA2ASemanticEnrichmentStep reactiveA2aStep;

    @Inject
    public DefaultReactiveSystemPromptRenderer(
            @Any final Instance<StreamingChatModel> streamingLlmInstance,
            final SystemPromptRenderer blockingDelegate,
            final EidosRenderPipeline pipeline,
            final ObjectMapper mapper) {
        this.streamingLlm = streamingLlmInstance.isResolvable() ? streamingLlmInstance.get() : null;
        this.blockingDelegate = blockingDelegate;
        this.pipeline = pipeline;
        this.reactiveEnrichStep = new ReactiveSemanticEnrichmentStep(mapper);
        this.reactiveA2aStep = new ReactiveA2ASemanticEnrichmentStep(mapper);
    }

    /** Package-private constructor for tests. */
    DefaultReactiveSystemPromptRenderer(
            final StreamingChatModel streamingLlm,
            final SystemPromptRenderer blockingDelegate,
            final EidosRenderPipeline pipeline,
            final ObjectMapper mapper) {
        this.streamingLlm = streamingLlm;
        this.blockingDelegate = blockingDelegate;
        this.pipeline = pipeline;
        this.reactiveEnrichStep = new ReactiveSemanticEnrichmentStep(mapper);
        this.reactiveA2aStep = new ReactiveA2ASemanticEnrichmentStep(mapper);
    }

    @Override
    public Uni<RenderedPrompt> render(final AgentDescriptor descriptor,
                                      final AgentPromptContext context) {
        if (streamingLlm == null) {
            // No streaming LLM: offload blocking render to worker pool (existing behaviour)
            return Uni.createFrom()
                      .item(() -> blockingDelegate.render(descriptor, context))
                      .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
        }

        // Stage 1 + cache check on worker pool — not on event loop
        return Uni.createFrom()
                  .item(() -> buildStageOne(descriptor, context))
                  .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                  .chain(s1 -> {
                      if (s1.cached() != null) return Uni.createFrom().item(s1.cached());
                      return executeStagesTwoAndThree(s1, descriptor, context);
                  });
    }

    private StageOneResult buildStageOne(final AgentDescriptor descriptor,
                                          final AgentPromptContext context) {
        final ObjectNode descriptorNode = pipeline.buildDescriptorPayload(descriptor);
        final ObjectNode contextNode    = pipeline.buildContextPayload(context);
        final String descriptorHash     = EidosRenderPipeline.fingerprint(descriptorNode.toString());
        final String contextHash        = EidosRenderPipeline.fingerprint(contextNode.toString());
        final String cacheKey           = pipeline.cacheKey(descriptorHash, contextHash, context.format());
        final RenderedPrompt cached     = pipeline.cacheGet(cacheKey).orElse(null);
        return new StageOneResult(descriptorNode, contextNode, descriptorHash, contextHash, cacheKey, cached);
    }

    private Uni<RenderedPrompt> executeStagesTwoAndThree(
            final StageOneResult s1,
            final AgentDescriptor descriptor,
            final AgentPromptContext context) {
        final RenderFormat format       = context.format();
        final boolean needsEnrichment   = EidosRenderPipeline.usesEnrichment(format);
        final boolean needsA2A          = format == RenderFormat.A2A_CARD;

        final ObjectNode llmPayload = needsEnrichment
                ? pipeline.buildLlmPayload(s1.descriptorNode(), s1.contextNode())
                : null;

        final Uni<Optional<SemanticEnrichment>> enrichUni = needsEnrichment
                ? reactiveEnrichStep.enrich(streamingLlm, llmPayload)
                : Uni.createFrom().item(Optional.empty());
        final Uni<Optional<A2AEnrichment>> a2aUni = needsA2A
                ? reactiveA2aStep.enrich(streamingLlm, s1.descriptorNode())
                : Uni.createFrom().item(Optional.empty());

        // Stage 3 assembly on worker pool — not on the streaming callback thread
        return Uni.combine().all().unis(enrichUni, a2aUni).asTuple()
                  .emitOn(Infrastructure.getDefaultWorkerPool())
                  .map(t -> pipeline.assembleAndCache(
                          s1.cacheKey(), s1.descriptorHash(), s1.contextHash(),
                          t.getItem1(), t.getItem2(), descriptor, context));
    }

    private record StageOneResult(
            ObjectNode descriptorNode,
            ObjectNode contextNode,
            String descriptorHash,
            String contextHash,
            String cacheKey,
            RenderedPrompt cached
    ) {}
}

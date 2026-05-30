package io.casehub.eidos.runtime.renderer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentPromptContext;
import io.casehub.eidos.api.ReactiveRenderedPromptCache;
import io.casehub.eidos.api.SystemPromptRenderer;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import dev.langchain4j.model.chat.ChatModel;

import java.time.Duration;
import java.util.Optional;

@DefaultBean
@ApplicationScoped
public class EidosSystemPromptRenderer implements SystemPromptRenderer {

    private final ChatModel llm;
    private final EidosRenderPipeline pipeline;
    private final SemanticEnrichmentStep enrichmentStep;
    private final A2ASemanticEnrichmentStep a2aEnrichmentStep;
    private final ReactiveRenderedPromptCache cache;

    @Inject
    public EidosSystemPromptRenderer(
            @Any final Instance<ChatModel> llm,
            final EidosRenderPipeline pipeline,
            final ReactiveRenderedPromptCache cache,
            final ObjectMapper mapper) {
        // ChatModel must be @ApplicationScoped (or broader). A @Dependent-scoped ChatModel
        // obtained via Instance.get() would leak. Quarkus LangChain4j always registers
        // ChatModel as @ApplicationScoped, so this is safe in practice.
        this.llm = llm.isResolvable() ? llm.get() : null;
        this.pipeline = pipeline;
        this.cache = cache;
        this.enrichmentStep = new SemanticEnrichmentStep(mapper);
        this.a2aEnrichmentStep = new A2ASemanticEnrichmentStep(mapper);
    }

    /** Package-private constructor for pure-Java tests — no CDI required. */
    EidosSystemPromptRenderer(final ChatModel llm,
                              final EidosRenderPipeline pipeline,
                              final ReactiveRenderedPromptCache cache,
                              final ObjectMapper mapper) {
        this.llm = llm;
        this.pipeline = pipeline;
        this.cache = cache;
        this.enrichmentStep = new SemanticEnrichmentStep(mapper);
        this.a2aEnrichmentStep = new A2ASemanticEnrichmentStep(mapper);
    }

    @Override
    public RenderedPrompt render(final AgentDescriptor descriptor, final AgentPromptContext context) {
        // Stage 1: build payloads and fingerprints
        final StageOneResult s1 = pipeline.buildStage1(descriptor, context);

        // Cache check — await() resolves synchronously for the default adapter (no runSubscriptionOn).
        // try-catch defends against future async implementations or adapter contract violations.
        final Optional<RenderedPrompt> cached;
        try {
            cached = cache.get(s1.lookupKey()).await().atMost(Duration.ofSeconds(5));
        } catch (final Exception e) {
            return renderFresh(s1, descriptor, context);
        }
        if (cached.isPresent()) return cached.get();
        return renderFresh(s1, descriptor, context);
    }

    private RenderedPrompt renderFresh(final StageOneResult s1,
                                       final AgentDescriptor descriptor,
                                       final AgentPromptContext context) {
        // Stage 2a: optional semantic enrichment
        Optional<SemanticEnrichment> enrichment = Optional.empty();
        if (llm != null && EidosRenderPipeline.usesEnrichment(context.format())) {
            final var llmPayload = pipeline.buildLlmPayload(s1.descriptorNode(), s1.contextNode());
            enrichment = enrichmentStep.enrich(llm, llmPayload);
        }

        // Stage 2b: A2A enrichment — descriptor-only payload, separate schema
        Optional<A2AEnrichment> a2aEnrichment = Optional.empty();
        if (context.format() == RenderFormat.A2A_CARD && llm != null) {
            a2aEnrichment = a2aEnrichmentStep.enrich(llm, s1.descriptorNode());
        }

        // Stage 3: format-specific assembly + cache put
        final RenderedPrompt result = pipeline.assemble(s1, enrichment, a2aEnrichment, descriptor, context);
        try {
            cache.put(s1.lookupKey(), result).await().atMost(Duration.ofSeconds(5));
        } catch (final Exception e) {
            // cache write failure — render result is already assembled, so swallow and return it
        }
        return result;
    }
}

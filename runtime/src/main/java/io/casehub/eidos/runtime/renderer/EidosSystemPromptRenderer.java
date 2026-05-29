package io.casehub.eidos.runtime.renderer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentPromptContext;
import io.casehub.eidos.api.SystemPromptRenderer;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import dev.langchain4j.model.chat.ChatModel;

import java.util.Optional;

@DefaultBean
@ApplicationScoped
public class EidosSystemPromptRenderer implements SystemPromptRenderer {

    private final ChatModel llm;
    private final EidosRenderPipeline pipeline;
    private final SemanticEnrichmentStep enrichmentStep;
    private final A2ASemanticEnrichmentStep a2aEnrichmentStep;

    @Inject
    public EidosSystemPromptRenderer(
            @Any final Instance<ChatModel> llm,
            final EidosRenderPipeline pipeline,
            final ObjectMapper mapper) {
        // ChatModel must be @ApplicationScoped (or broader). A @Dependent-scoped ChatModel
        // obtained via Instance.get() would leak. Quarkus LangChain4j always registers
        // ChatModel as @ApplicationScoped, so this is safe in practice.
        this.llm = llm.isResolvable() ? llm.get() : null;
        this.pipeline = pipeline;
        this.enrichmentStep = new SemanticEnrichmentStep(mapper);
        this.a2aEnrichmentStep = new A2ASemanticEnrichmentStep(mapper);
    }

    /** Package-private constructor for pure-Java tests — no CDI required. */
    EidosSystemPromptRenderer(final ChatModel llm,
                              final EidosRenderPipeline pipeline,
                              final ObjectMapper mapper) {
        this.llm = llm;
        this.pipeline = pipeline;
        this.enrichmentStep = new SemanticEnrichmentStep(mapper);
        this.a2aEnrichmentStep = new A2ASemanticEnrichmentStep(mapper);
    }

    @Override
    public RenderedPrompt render(final AgentDescriptor descriptor, final AgentPromptContext context) {
        final ObjectNode descriptorNode = pipeline.buildDescriptorPayload(descriptor);
        final ObjectNode contextNode    = pipeline.buildContextPayload(context);
        final String descriptorHash     = EidosRenderPipeline.fingerprint(descriptorNode.toString());
        final String contextHash        = EidosRenderPipeline.fingerprint(contextNode.toString());
        final String cacheKey           = pipeline.cacheKey(descriptorHash, contextHash, context.format());

        final Optional<RenderedPrompt> cached = pipeline.cacheGet(cacheKey);
        if (cached.isPresent()) return cached.get();

        // Stage 2a: optional semantic enrichment
        Optional<SemanticEnrichment> enrichment = Optional.empty();
        if (llm != null && EidosRenderPipeline.usesEnrichment(context.format())) {
            final ObjectNode llmPayload = pipeline.buildLlmPayload(descriptorNode, contextNode);
            enrichment = enrichmentStep.enrich(llm, llmPayload);
        }

        // Stage 2b: A2A enrichment — descriptor-only payload, separate schema
        Optional<A2AEnrichment> a2aEnrichment = Optional.empty();
        if (context.format() == RenderFormat.A2A_CARD && llm != null) {
            a2aEnrichment = a2aEnrichmentStep.enrich(llm, descriptorNode);
        }

        // Stage 3: format-specific assembly + cache
        return pipeline.assembleAndCache(cacheKey, descriptorHash, contextHash,
                enrichment, a2aEnrichment, descriptor, context);
    }
}

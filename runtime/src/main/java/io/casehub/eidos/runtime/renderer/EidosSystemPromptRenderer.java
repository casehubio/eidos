package io.casehub.eidos.runtime.renderer;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentPromptContext;
import io.casehub.eidos.api.CoherenceLevel;
import io.casehub.eidos.api.RenderedPromptCache;
import io.casehub.eidos.api.SystemPromptRenderer;
import io.casehub.eidos.runtime.validator.BriefingCoherenceValidator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.Optional;

@ApplicationScoped
public class EidosSystemPromptRenderer implements SystemPromptRenderer {

    private final ChatModel                    llm;
    private final EidosRenderPipeline          pipeline;
    private final SemanticEnrichmentStep       enrichmentStep;
    private final A2ASemanticEnrichmentStep    a2aEnrichmentStep;
    private final RenderedPromptCache          cache;
    private final BriefingCoherenceValidator   coherenceValidator;

    @Inject
    public EidosSystemPromptRenderer(
            @Any final Instance<ChatModel> llm,
            final EidosRenderPipeline pipeline,
            final RenderedPromptCache cache,
            final ObjectMapper mapper,
            final BriefingCoherenceValidator coherenceValidator) {
        this.llm                = llm.isResolvable() ? llm.get() : null;
        this.pipeline           = pipeline;
        this.cache              = cache;
        this.enrichmentStep     = new SemanticEnrichmentStep(mapper);
        this.a2aEnrichmentStep  = new A2ASemanticEnrichmentStep(mapper);
        this.coherenceValidator = coherenceValidator;
    }

    EidosSystemPromptRenderer(final ChatModel llm,
                              final EidosRenderPipeline pipeline,
                              final RenderedPromptCache cache,
                              final ObjectMapper mapper) {
        this.llm                = llm;
        this.pipeline           = pipeline;
        this.cache              = cache;
        this.enrichmentStep     = new SemanticEnrichmentStep(mapper);
        this.a2aEnrichmentStep  = new A2ASemanticEnrichmentStep(mapper);
        this.coherenceValidator = null;
    }

    @Override
    public RenderedPrompt render(final AgentDescriptor descriptor, final AgentPromptContext context) {
        final StageOneResult s1 = pipeline.buildStage1(descriptor, context);

        final Optional<RenderedPrompt> cached;
        try {
            cached = cache.get(s1.lookupKey());
        } catch (final Exception e) {
            return renderFresh(s1, descriptor, context);
        }
        if (cached.isPresent()) {return cached.get();}
        return renderFresh(s1, descriptor, context);
    }

    private RenderedPrompt renderFresh(final StageOneResult s1,
                                       final AgentDescriptor descriptor,
                                       final AgentPromptContext context) {
        Optional<SemanticEnrichment> enrichment = Optional.empty();
        if (llm != null && EidosRenderPipeline.usesEnrichment(context.format())) {
            final var llmPayload = pipeline.buildEnrichmentPayload(s1.descriptorNode(), s1.contextNode());
            enrichment = enrichmentStep.enrich(llm, llmPayload);
        }

        Optional<A2AEnrichment> a2aEnrichment = Optional.empty();
        if (context.format() == RenderFormat.A2A_CARD && llm != null) {
            a2aEnrichment = a2aEnrichmentStep.enrich(llm, s1.descriptorNode());
        }

        RenderedPrompt result = pipeline.assemble(s1, enrichment, a2aEnrichment, descriptor, context);

        if (coherenceValidator != null) {
            var coherence = coherenceValidator.validateStructural(descriptor);
            if (coherence.overall() != CoherenceLevel.ALIGNED) {
                result = new RenderedPrompt(result.content(), result.format(),
                    result.descriptorHash(), result.contextHash(), result.enriched(), coherence);
            }
        }

        try {
            cache.put(s1.lookupKey(), result);
        } catch (final Exception e) {
            // cache write failure — render result is already assembled
        }
        return result;
    }
}

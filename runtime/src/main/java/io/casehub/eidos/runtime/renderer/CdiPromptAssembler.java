package io.casehub.eidos.runtime.renderer;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentPromptContext;
import io.casehub.eidos.api.AssembledPrompt;
import io.casehub.eidos.api.PromptAssembler;
import io.casehub.eidos.api.SystemPromptRenderer;
import io.casehub.eidos.api.spi.PromptContributor;
import io.casehub.eidos.core.renderer.DefaultPromptAssembler;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class CdiPromptAssembler implements PromptAssembler {

    @Inject
    SystemPromptRenderer renderer;

    @Inject
    Instance<PromptContributor> contributorInstances;

    private List<PromptContributor> contributors;

    @PostConstruct
    void init() {
        contributors = contributorInstances.stream().toList();
    }

    @Override
    public AssembledPrompt assemble(AgentDescriptor descriptor, AgentPromptContext context) {
        var delegate = new DefaultPromptAssembler(
                (desc, ctx) -> renderer.render(desc, ctx).content(),
                contributors);
        return delegate.assemble(descriptor, context);
    }
}

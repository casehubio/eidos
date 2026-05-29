package io.casehub.eidos.runtime.renderer;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentPromptContext;
import io.casehub.eidos.api.ReactiveSystemPromptRenderer;
import io.casehub.eidos.api.SystemPromptRenderer;
import io.casehub.eidos.api.SystemPromptRenderer.RenderedPrompt;
import io.quarkus.arc.DefaultBean;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@DefaultBean
@ApplicationScoped
public class DefaultReactiveSystemPromptRenderer implements ReactiveSystemPromptRenderer {

    @Inject
    SystemPromptRenderer delegate;

    @Override
    public Uni<RenderedPrompt> render(final AgentDescriptor descriptor, final AgentPromptContext context) {
        return Uni.createFrom()
                  .item(() -> delegate.render(descriptor, context))
                  .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }
}

package io.casehub.eidos.api;

import io.smallrye.mutiny.Uni;

public interface ReactiveSystemPromptRenderer {
    Uni<SystemPromptRenderer.RenderedPrompt> render(AgentDescriptor descriptor, AgentPromptContext context);
}

package io.casehub.eidos.runtime.renderer;

import io.casehub.eidos.api.RenderedPromptCache;
import io.casehub.eidos.api.SystemPromptRenderer.RenderedPrompt;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@DefaultBean
@ApplicationScoped
public class NoOpRenderedPromptCache implements RenderedPromptCache {

    @Override
    public Optional<RenderedPrompt> get(final String cacheKey) {
        return Optional.empty();
    }

    @Override
    public void put(final String cacheKey, final RenderedPrompt result) {}
}

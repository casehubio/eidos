package io.casehub.eidos.core.renderer;

import io.casehub.eidos.api.RenderedPromptCache;
import io.casehub.eidos.api.SystemPromptRenderer.RenderedPrompt;


import java.util.Optional;


public class NoOpRenderedPromptCache implements RenderedPromptCache {

    @Override
    public Optional<RenderedPrompt> get(final String cacheKey) {
        return Optional.empty();
    }

    @Override
    public void put(final String cacheKey, final RenderedPrompt result) {}
}

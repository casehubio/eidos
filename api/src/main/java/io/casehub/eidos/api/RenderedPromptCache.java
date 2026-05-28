package io.casehub.eidos.api;

import java.util.Optional;

public interface RenderedPromptCache {
    Optional<SystemPromptRenderer.RenderedPrompt> get(String cacheKey);

    /**
     * Stores a rendered prompt. Must not throw — implementations handle errors internally
     * so a cache failure never aborts a render.
     */
    void put(String cacheKey, SystemPromptRenderer.RenderedPrompt result);
}

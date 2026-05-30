package io.casehub.eidos.api;

import io.smallrye.mutiny.Uni;
import java.util.Optional;

public interface ReactiveRenderedPromptCache {

    /**
     * Returns the cached prompt for the given key.
     * On failure, implementations must recover and return Uni of Optional.empty() —
     * a cache miss must never abort a render.
     */
    Uni<Optional<SystemPromptRenderer.RenderedPrompt>> get(String cacheKey);

    /**
     * Stores a rendered prompt. Must not emit a failure — implementations recover
     * internally so a cache write failure never aborts a render.
     */
    Uni<Void> put(String cacheKey, SystemPromptRenderer.RenderedPrompt result);
}

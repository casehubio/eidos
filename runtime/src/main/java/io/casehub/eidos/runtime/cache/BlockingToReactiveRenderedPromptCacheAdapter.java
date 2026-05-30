package io.casehub.eidos.runtime.cache;

import io.casehub.eidos.api.ReactiveRenderedPromptCache;
import io.casehub.eidos.api.RenderedPromptCache;
import io.casehub.eidos.api.SystemPromptRenderer.RenderedPrompt;
import io.quarkus.arc.DefaultBean;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;

@DefaultBean
@ApplicationScoped
class BlockingToReactiveRenderedPromptCacheAdapter implements ReactiveRenderedPromptCache {

    private final RenderedPromptCache blocking;

    @Inject
    BlockingToReactiveRenderedPromptCacheAdapter(final RenderedPromptCache blocking) {
        this.blocking = blocking;
    }

    // Package-private constructor for tests (no CDI).
    BlockingToReactiveRenderedPromptCacheAdapter(final RenderedPromptCache blocking,
                                                  @SuppressWarnings("unused") boolean testMarker) {
        this.blocking = blocking;
    }

    @Override
    public Uni<Optional<RenderedPrompt>> get(final String cacheKey) {
        // No runSubscriptionOn — callers (blocking renderer, reactive Stage 1) are already
        // on the worker pool. Adding runSubscriptionOn(workerPool) here risks deadlock
        // under saturation (calling thread blocks waiting for a hop on the same pool).
        return Uni.createFrom()
                  .item(() -> blocking.get(cacheKey))
                  .onFailure().recoverWithItem(e -> Optional.empty());
    }

    @Override
    public Uni<Void> put(final String cacheKey, final RenderedPrompt result) {
        return Uni.createFrom()
                  .<Void>item(() -> { blocking.put(cacheKey, result); return null; })
                  .onFailure().recoverWithNull()
                  .replaceWithVoid();
    }
}

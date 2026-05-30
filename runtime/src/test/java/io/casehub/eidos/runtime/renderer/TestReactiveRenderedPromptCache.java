package io.casehub.eidos.runtime.renderer;

import io.casehub.eidos.api.ReactiveRenderedPromptCache;
import io.casehub.eidos.api.SystemPromptRenderer.RenderedPrompt;
import io.smallrye.mutiny.Uni;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class TestReactiveRenderedPromptCache implements ReactiveRenderedPromptCache {
    final Map<String, RenderedPrompt> store = new HashMap<>();
    int putCount = 0;
    int getCount = 0;

    @Override
    public Uni<Optional<RenderedPrompt>> get(final String cacheKey) {
        getCount++;
        return Uni.createFrom().item(Optional.ofNullable(store.get(cacheKey)));
    }

    @Override
    public Uni<Void> put(final String cacheKey, final RenderedPrompt result) {
        putCount++;
        store.put(cacheKey, result);
        return Uni.createFrom().voidItem();
    }
}

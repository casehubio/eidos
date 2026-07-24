package io.casehub.eidos.runtime.renderer;

import io.casehub.eidos.api.RenderedPromptCache;
import io.casehub.eidos.api.SystemPromptRenderer.RenderedPrompt;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

class TestRenderedPromptCache implements RenderedPromptCache {

    private final ConcurrentHashMap<String, RenderedPrompt> store = new ConcurrentHashMap<>();
    int getCount;
    int putCount;

    @Override
    public Optional<RenderedPrompt> get(final String cacheKey) {
        getCount++;
        return Optional.ofNullable(store.get(cacheKey));
    }

    @Override
    public void put(final String cacheKey, final RenderedPrompt result) {
        putCount++;
        store.put(cacheKey, result);
    }
}

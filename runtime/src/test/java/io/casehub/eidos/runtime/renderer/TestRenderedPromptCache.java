package io.casehub.eidos.runtime.renderer;

import io.casehub.eidos.api.RenderedPromptCache;
import io.casehub.eidos.api.SystemPromptRenderer.RenderedPrompt;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class TestRenderedPromptCache implements RenderedPromptCache {
    final Map<String, RenderedPrompt> store = new HashMap<>();
    int putCount = 0;
    int getCount = 0;

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

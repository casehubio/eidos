package io.casehub.eidos.memory;

import io.casehub.eidos.api.RenderedPromptCache;
import io.casehub.eidos.api.SystemPromptRenderer.RenderedPrompt;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Alternative
@Priority(1)
@ApplicationScoped
public class InMemoryRenderedPromptCache implements RenderedPromptCache {

    @ConfigProperty(name = "casehub.eidos.renderer.cache-size", defaultValue = "256")
    int maxSize;

    private Map<String, RenderedPrompt> cache;

    @PostConstruct
    void init() {
        // Capture maxSize into a final local — removeEldestEntry must not capture
        // the mutable field directly. Collections.synchronizedMap serialises get/put
        // (including access-order reordering by LinkedHashMap), so concurrent use is safe.
        final int limit = maxSize;
        cache = Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(final Map.Entry<String, RenderedPrompt> eldest) {
                    return size() > limit;
                }
            }
        );
    }

    @Override
    public Optional<RenderedPrompt> get(final String cacheKey) {
        return Optional.ofNullable(cache.get(cacheKey));
    }

    @Override
    public void put(final String cacheKey, final RenderedPrompt result) {
        cache.put(cacheKey, result);
    }
}

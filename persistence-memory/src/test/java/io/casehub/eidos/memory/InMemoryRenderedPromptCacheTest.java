package io.casehub.eidos.memory;

import io.casehub.eidos.api.SystemPromptRenderer.RenderedPrompt;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRenderedPromptCacheTest {

    InMemoryRenderedPromptCache cache;

    @BeforeEach
    void setUp() {
        cache = new InMemoryRenderedPromptCache();
        cache.maxSize = 3;
        cache.init();
    }

    static RenderedPrompt prompt(String content) {
        return new RenderedPrompt(content, RenderFormat.MARKDOWN, "dh", "ch", false);
    }

    @Test
    void miss_returns_empty() {
        assertThat(cache.get("missing")).isEmpty();
    }

    @Test
    void put_then_get_returns_value() {
        cache.put("k1", prompt("hello"));
        assertThat(cache.get("k1")).contains(prompt("hello"));
    }

    @Test
    void evicts_least_recently_used_when_full() {
        cache.put("k1", prompt("one"));
        cache.put("k2", prompt("two"));
        cache.put("k3", prompt("three"));
        // access k1 to make k2 the LRU
        cache.get("k1");
        cache.get("k3");
        // adding k4 should evict k2
        cache.put("k4", prompt("four"));
        assertThat(cache.get("k2")).isEmpty();
        assertThat(cache.get("k1")).isPresent();
        assertThat(cache.get("k3")).isPresent();
        assertThat(cache.get("k4")).isPresent();
    }

    @Test
    void different_keys_are_independent() {
        cache.put("a", prompt("alpha"));
        cache.put("b", prompt("beta"));
        assertThat(cache.get("a")).contains(prompt("alpha"));
        assertThat(cache.get("b")).contains(prompt("beta"));
    }
}

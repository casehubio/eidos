package io.casehub.eidos.runtime.cache;

import io.casehub.eidos.api.RenderedPromptCache;
import io.casehub.eidos.api.ReactiveRenderedPromptCache;
import io.casehub.eidos.api.SystemPromptRenderer.RenderedPrompt;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class BlockingToReactiveRenderedPromptCacheAdapterTest {

    static final RenderedPrompt PROMPT = new RenderedPrompt("content", RenderFormat.MARKDOWN, "dh", "ch");

    ReactiveRenderedPromptCache adapter;

    @BeforeEach
    void setUp() {
        adapter = new BlockingToReactiveRenderedPromptCacheAdapter(new InMemoryBlockingCache(), true);
    }

    @Test
    void get_returns_empty_on_miss() {
        assertThat(adapter.get("missing").await().indefinitely()).isEmpty();
    }

    @Test
    void put_then_get_returns_stored_value() {
        adapter.put("key", PROMPT).await().indefinitely();
        assertThat(adapter.get("key").await().indefinitely()).contains(PROMPT);
    }

    @Test
    void get_returns_empty_when_blocking_cache_throws() {
        final ReactiveRenderedPromptCache failingAdapter =
            new BlockingToReactiveRenderedPromptCacheAdapter(new ThrowingBlockingCache(), true);
        assertThat(failingAdapter.get("key").await().indefinitely()).isEmpty();
    }

    @Test
    void put_completes_when_blocking_cache_throws() {
        final ReactiveRenderedPromptCache failingAdapter =
            new BlockingToReactiveRenderedPromptCacheAdapter(new ThrowingBlockingCache(), true);
        // must not throw — Uni must complete normally
        failingAdapter.put("key", PROMPT).await().indefinitely();
    }

    // ── test doubles ──────────────────────────────────────────────────────────

    static class InMemoryBlockingCache implements RenderedPromptCache {
        private final java.util.Map<String, RenderedPrompt> store = new java.util.HashMap<>();

        @Override public Optional<RenderedPrompt> get(String k) { return Optional.ofNullable(store.get(k)); }
        @Override public void put(String k, RenderedPrompt v)   { store.put(k, v); }
    }

    static class ThrowingBlockingCache implements RenderedPromptCache {
        @Override public Optional<RenderedPrompt> get(String k) { throw new RuntimeException("cache down"); }
        @Override public void put(String k, RenderedPrompt v)   { throw new RuntimeException("cache down"); }
    }
}

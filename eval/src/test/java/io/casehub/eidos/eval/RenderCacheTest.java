package io.casehub.eidos.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import io.casehub.eidos.api.SystemPromptRenderer.RenderedPrompt;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class RenderCacheTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void renderCacheEntry_round_trips_through_json() throws Exception {
        final var entry = new RenderCacheEntry("advisor-high-markdown", "MARKDOWN", "You are an advisor.");
        final String json = mapper.writeValueAsString(List.of(entry));
        final RenderCacheEntry[] loaded = mapper.readValue(json, RenderCacheEntry[].class);

        assertThat(loaded).hasSize(1);
        assertThat(loaded[0].caseName()).isEqualTo("advisor-high-markdown");
        assertThat(loaded[0].format()).isEqualTo("MARKDOWN");
        assertThat(loaded[0].content()).isEqualTo("You are an advisor.");
    }

    @Test
    void renderCacheEntry_reconstructs_renderedPrompt() {
        final var entry = new RenderCacheEntry("planner-low-prose", "PROSE", "Act as a planner.");
        final RenderedPrompt prompt = entry.toRenderedPrompt();

        assertThat(prompt.content()).isEqualTo("Act as a planner.");
        assertThat(prompt.format()).isEqualTo(RenderFormat.PROSE);
        assertThat(prompt.descriptorHash()).isNull();
        assertThat(prompt.contextHash()).isNull();
    }

    @Test
    void renderCacheEntry_list_covers_all_formats() {
        final var entries = List.of(
            new RenderCacheEntry("x-markdown", "MARKDOWN", "md content"),
            new RenderCacheEntry("x-prose", "PROSE", "prose content")
        );
        final Map<RenderFormat, String> byFormat = entries.stream()
            .collect(Collectors.toMap(
                e -> RenderFormat.valueOf(e.format()),
                RenderCacheEntry::content));

        assertThat(byFormat).containsKeys(RenderFormat.MARKDOWN, RenderFormat.PROSE);
        assertThat(byFormat.get(RenderFormat.MARKDOWN)).isEqualTo("md content");
    }
}

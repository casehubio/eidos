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
        final var entry = new RenderCacheEntry("advisor-high-markdown", "MARKDOWN", "You are an advisor.", false);
        final String json = mapper.writeValueAsString(List.of(entry));
        final RenderCacheEntry[] loaded = mapper.readValue(json, RenderCacheEntry[].class);

        assertThat(loaded).hasSize(1);
        assertThat(loaded[0].caseName()).isEqualTo("advisor-high-markdown");
        assertThat(loaded[0].format()).isEqualTo("MARKDOWN");
        assertThat(loaded[0].content()).isEqualTo("You are an advisor.");
    }

    @Test
    void renderCacheEntry_reconstructs_renderedPrompt() {
        final var entry = new RenderCacheEntry("planner-low-prose", "PROSE", "Act as a planner.", false);
        final RenderedPrompt prompt = entry.toRenderedPrompt();

        assertThat(prompt.content()).isEqualTo("Act as a planner.");
        assertThat(prompt.format()).isEqualTo(RenderFormat.PROSE);
        assertThat(prompt.descriptorHash()).isNull();
        assertThat(prompt.contextHash()).isNull();
        assertThat(prompt.enriched()).isFalse();
    }

    @Test
    void renderCacheEntry_preserves_enriched_flag_on_round_trip() throws Exception {
        final var entry = new RenderCacheEntry("advisor-high-markdown", "MARKDOWN", "You are enriched.", true);
        final String json = mapper.writeValueAsString(List.of(entry));
        final RenderCacheEntry[] loaded = mapper.readValue(json, RenderCacheEntry[].class);

        assertThat(loaded[0].enriched()).isTrue();
        assertThat(loaded[0].toRenderedPrompt().enriched()).isTrue();
    }

    @Test
    void renderCacheEntry_enriched_defaults_to_false_for_legacy_json_without_field() throws Exception {
        final String legacyJson = "[{\"caseName\":\"x\",\"format\":\"MARKDOWN\",\"content\":\"old\"}]";
        final RenderCacheEntry[] loaded = mapper.readValue(legacyJson, RenderCacheEntry[].class);
        assertThat(loaded[0].enriched()).isFalse();
        assertThat(loaded[0].toRenderedPrompt().enriched()).isFalse();
    }

    @Test
    void renderCacheEntry_list_covers_all_formats() {
        final var entries = List.of(
            new RenderCacheEntry("x-markdown", "MARKDOWN", "md content", false),
            new RenderCacheEntry("x-prose", "PROSE", "prose content", false)
        );
        final Map<RenderFormat, String> byFormat = entries.stream()
            .collect(Collectors.toMap(
                e -> RenderFormat.valueOf(e.format()),
                RenderCacheEntry::content));

        assertThat(byFormat).containsKeys(RenderFormat.MARKDOWN, RenderFormat.PROSE);
        assertThat(byFormat.get(RenderFormat.MARKDOWN)).isEqualTo("md content");
    }
}

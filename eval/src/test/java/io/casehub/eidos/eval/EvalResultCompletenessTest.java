package io.casehub.eidos.eval;

import io.casehub.eidos.api.*;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import io.casehub.eidos.api.SystemPromptRenderer.RenderedPrompt;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EvalResultCompletenessTest {

    static AgentDescriptor descriptorWithCaps(final String... capNames) {
        final var caps = Arrays.stream(capNames)
            .map(n -> new AgentCapability(n, null, null, null,
                List.of(), List.of(), List.of(), Map.of()))
            .toList();
        return new AgentDescriptor("id", "Name", null, null, null, null, null,
            null, null, null, null, "worker", caps, null, null, null, "tenant");
    }

    static boolean computeCompleteness(final AgentDescriptor descriptor, final String renderedContent) {
        return descriptor.capabilities().stream()
            .allMatch(cap -> renderedContent.contains(cap.name()));
    }

    static List<String> missingCapabilities(final AgentDescriptor descriptor, final String renderedContent) {
        return descriptor.capabilities().stream()
            .map(AgentCapability::name)
            .filter(name -> !renderedContent.contains(name))
            .toList();
    }

    @Test
    void all_caps_present_returns_true() {
        final var desc = descriptorWithCaps("code-review", "estimation");
        final String content = "You can perform code-review and estimation tasks.";
        assertThat(computeCompleteness(desc, content)).isTrue();
        assertThat(missingCapabilities(desc, content)).isEmpty();
    }

    @Test
    void missing_cap_returns_false() {
        final var desc = descriptorWithCaps("code-review", "estimation");
        final String content = "You can perform code-review tasks.";
        assertThat(computeCompleteness(desc, content)).isFalse();
        assertThat(missingCapabilities(desc, content)).containsExactly("estimation");
    }

    @Test
    void no_caps_returns_true() {
        final var desc = descriptorWithCaps();
        assertThat(computeCompleteness(desc, "any content")).isTrue();
        assertThat(missingCapabilities(desc, "any content")).isEmpty();
    }
}

package io.casehub.eidos.memory;

import io.casehub.eidos.api.*;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@QuarkusTest
class InMemoryAgentRegistryTest {

    @Inject AgentRegistry registry;

    static AgentDescriptor descriptor(String agentId, String slot, String tenancyId, String... caps) {
        var capabilities = Arrays.stream(caps)
            .map(n -> new AgentCapability(n, 0.9, null, null,
                List.of(), List.of(), List.of(), Map.of()))
            .toList();
        return new AgentDescriptor(
            agentId, "Agent", "1.0", "anthropic", "claude", "claude-3-7",
            null, null, null, null, slot, capabilities,
            new AgentDisposition("collaborative", "principled", "measured", "semi-autonomous", false),
            null, null, tenancyId
        );
    }

    @Test
    void register_and_find_by_id() {
        registry.register(descriptor("m-1", "reviewer", "default", "code-review"));
        var found = registry.findById("m-1", "default");
        assertThat(found).isPresent();
        assertThat(found.get().slot()).isEqualTo("reviewer");
        assertThat(found.get().tenancyId()).isEqualTo("default");
    }

    @Test
    void find_by_slot() {
        registry.register(descriptor("m-2a", "reviewer", "default", "code-review"));
        registry.register(descriptor("m-2b", "planner", "default", "planning"));
        var result = registry.find(AgentQuery.bySlot("reviewer", "default"));
        assertThat(result.stream().map(AgentDescriptor::agentId).toList())
            .contains("m-2a").doesNotContain("m-2b");
    }

    @Test
    void find_by_capability() {
        registry.register(descriptor("m-3a", "reviewer", "default", "code-review"));
        registry.register(descriptor("m-3b", "executor", "default", "testing"));
        var result = registry.find(AgentQuery.byCapability("code-review", "default"));
        assertThat(result.stream().map(AgentDescriptor::agentId).toList())
            .contains("m-3a").doesNotContain("m-3b");
    }

    @Test
    void find_by_slot_and_capability() {
        registry.register(descriptor("m-4a", "reviewer", "default", "code-review"));
        registry.register(descriptor("m-4b", "executor", "default", "code-review"));
        var result = registry.find(AgentQuery.bySlotAndCapability("reviewer", "code-review", "default"));
        assertThat(result.stream().map(AgentDescriptor::agentId).toList())
            .contains("m-4a").doesNotContain("m-4b");
    }

    @Test
    void tenancy_isolation() {
        registry.register(descriptor("m-5", "reviewer", "tenant-a", "code-review"));
        assertThat(registry.find(AgentQuery.bySlot("reviewer", "tenant-b"))).isEmpty();
    }

    @Test
    void upsert_replaces_existing() {
        registry.register(descriptor("m-6", "reviewer", "default", "code-review"));
        registry.register(descriptor("m-6", "planner", "default", "planning"));
        assertThat(registry.findById("m-6", "default").get().slot()).isEqualTo("planner");
    }
}

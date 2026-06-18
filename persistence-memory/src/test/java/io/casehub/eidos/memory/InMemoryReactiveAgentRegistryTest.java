package io.casehub.eidos.memory;

import io.casehub.eidos.api.*;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@QuarkusTest
class InMemoryReactiveAgentRegistryTest {

    @Inject ReactiveAgentRegistry registry;

    static AgentDescriptor descriptor(String agentId, String slot, String tenancyId) {
        return AgentDescriptor.builder()
            .agentId(agentId)
            .name("Agent")
            .version("1.0")
            .provider("anthropic")
            .modelFamily("claude")
            .modelVersion("claude-3-7")
            .slot(slot)
            .capabilities(List.of(AgentCapability.builder().name("cap").qualityHint(0.9)
                .epistemicDomains(Map.of()).build()))
            .disposition(AgentDisposition.builder()
                .socialOrient("collaborative")
                .ruleFollowing("principled")
                .riskAppetite("measured")
                .autonomy("semi-autonomous")
                .build())
            .tenancyId(tenancyId)
            .build();
    }

    @Test
    void register_and_find_by_id() {
        registry.register(descriptor("rm-1", "reviewer", "default")).await().indefinitely();
        var found = registry.findById("rm-1", "default").await().indefinitely();
        assertThat(found).isPresent();
        assertThat(found.get().slot()).isEqualTo("reviewer");
    }

    @Test
    void find_by_slot() {
        registry.register(descriptor("rm-2a", "reviewer", "default")).await().indefinitely();
        registry.register(descriptor("rm-2b", "planner", "default")).await().indefinitely();
        var result = registry.find(AgentQuery.bySlot("reviewer", "default")).await().indefinitely();
        assertThat(result.stream().map(AgentDescriptor::agentId).toList())
            .contains("rm-2a").doesNotContain("rm-2b");
    }

    @Test
    void tenancy_isolation() {
        registry.register(descriptor("rm-3", "reviewer", "tenant-a")).await().indefinitely();
        var result = registry.find(AgentQuery.bySlot("reviewer", "tenant-b")).await().indefinitely();
        assertThat(result).isEmpty();
    }

    @Test
    void findById_with_null_agentId_throws() {
        assertThatNullPointerException()
            .isThrownBy(() -> registry.findById(null, "default"))
            .withMessageContaining("agentId");
    }

    @Test
    void findById_with_null_tenancyId_throws() {
        registry.register(descriptor("rm-10", "reviewer", "default")).await().indefinitely();
        assertThatThrownBy(() -> registry.findById("rm-10", null))
            .isInstanceOf(NullPointerException.class);
    }
}

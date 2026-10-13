package io.casehub.eidos.memory;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentDisposition;
import io.casehub.eidos.api.AgentRegistry;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class InMemoryAgentRegistryEventTest {

    @Inject AgentRegistry registry;
    @Inject InMemoryAgentRegistry store;
    @Inject TestAgentRegistryEvents collector;

    @BeforeEach
    void setUp() {
        store.clear();
        collector.clear();
    }

    static AgentDescriptor descriptor(String agentId, String slot, String tenancyId) {
        return AgentDescriptor.builder()
                              .agentId(agentId)
                              .name("Agent")
                              .version("1.0")
                              .provider("anthropic")
                              .modelFamily("claude")
                              .modelVersion("claude-3-7")
                              .slot(slot)
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
    void register_new_fires_registered_event() {
        var desc = descriptor("evt-1", "reviewer", "default");
        registry.register(desc);

        assertThat(collector.registered).hasSize(1);
        assertThat(collector.registered.getFirst().descriptor()).isEqualTo(desc);
        assertThat(collector.updated).isEmpty();
        assertThat(collector.removed).isEmpty();
    }

    @Test
    void register_existing_fires_updated_event() {
        var first = descriptor("evt-2", "reviewer", "default");
        registry.register(first);
        collector.clear();

        var second = descriptor("evt-2", "planner", "default");
        registry.register(second);

        assertThat(collector.updated).hasSize(1);
        assertThat(collector.updated.getFirst().previous().slot()).isEqualTo("reviewer");
        assertThat(collector.updated.getFirst().current().slot()).isEqualTo("planner");
        assertThat(collector.registered).isEmpty();
    }

    @Test
    void deregister_existing_fires_removed_event() {
        registry.register(descriptor("evt-3", "reviewer", "default"));
        collector.clear();

        registry.deregister("evt-3", "default");

        assertThat(collector.removed).hasSize(1);
        assertThat(collector.removed.getFirst().agentId()).isEqualTo("evt-3");
        assertThat(collector.removed.getFirst().tenancyId()).isEqualTo("default");
    }

    @Test
    void deregister_nonexistent_fires_no_event() {
        registry.deregister("nonexistent", "default");

        assertThat(collector.removed).isEmpty();
    }

    @Test
    void deregister_wrong_tenancy_fires_no_event() {
        registry.register(descriptor("evt-4", "reviewer", "tenant-a"));
        collector.clear();

        registry.deregister("evt-4", "tenant-b");

        assertThat(collector.removed).isEmpty();
        assertThat(registry.findById("evt-4", "tenant-a")).isPresent();
    }

    @Test
    void deregister_removes_from_registry() {
        registry.register(descriptor("evt-5", "reviewer", "default"));

        registry.deregister("evt-5", "default");

        assertThat(registry.findById("evt-5", "default")).isEmpty();
    }
}

package io.casehub.eidos.runtime.registry;

import io.casehub.eidos.api.*;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@QuarkusTest
@TestProfile(ReactiveTestProfile.class)
class JpaReactiveAgentRegistryTest {

    @Inject
    ReactiveAgentRegistry registry;

    static AgentDescriptor descriptor(String agentId, String slot, String tenancyId,
                                      String... capabilityNames) {
        var caps = Arrays.stream(capabilityNames)
            .map(n -> new AgentCapability(n, 0.9, null, null,
                List.of(), List.of(), List.of(), Map.of()))
            .toList();
        return new AgentDescriptor(
            agentId, "Agent " + agentId, "1.0", "anthropic",
            "claude", "claude-3-7", null,
            null, null, null,
            slot, caps,
            new AgentDisposition("collaborative", "principled", "measured", "semi-autonomous", false),
            null, null, tenancyId
        );
    }

    @Test
    @RunOnVertxContext
    void reactive_register_and_find_by_id(UniAsserter asserter) {
        asserter.execute(() -> registry.register(descriptor("r-agent-1", "reviewer", "default", "cap-test1")))
            .assertThat(() -> registry.findById("r-agent-1", "default"), found -> {
                assertThat(found).isPresent();
                assertThat(found.get().slot()).isEqualTo("reviewer");
                assertThat(found.get().tenancyId()).isEqualTo("default");
            });
    }

    @Test
    @RunOnVertxContext
    void reactive_find_by_slot(UniAsserter asserter) {
        asserter
            .execute(() -> registry.register(descriptor("r-agent-2a", "reviewer", "default", "cap-test2a")))
            .execute(() -> registry.register(descriptor("r-agent-2b", "planner", "default", "cap-test2b")))
            .assertThat(() -> registry.find(AgentQuery.bySlot("reviewer", "default")), result -> {
                // at least r-agent-2a present; r-agent-1 may also be here — filter to known set
                var agentIds = result.stream().map(AgentDescriptor::agentId).toList();
                assertThat(agentIds).contains("r-agent-2a");
                assertThat(agentIds).doesNotContain("r-agent-2b");
            });
    }

    @Test
    @RunOnVertxContext
    void reactive_find_by_capability(UniAsserter asserter) {
        // Use a unique capability name so cross-test data doesn't pollute the result
        asserter
            .execute(() -> registry.register(descriptor("r-agent-3", "reviewer", "default", "cap-test3-unique")))
            .assertThat(() -> registry.find(AgentQuery.byCapability("cap-test3-unique", "default")),
                result -> assertThat(result).hasSize(1));
    }

    @Test
    @RunOnVertxContext
    void reactive_tenancy_isolation(UniAsserter asserter) {
        asserter
            .execute(() -> registry.register(descriptor("r-agent-4", "reviewer", "tenant-a", "cap-test4")))
            .assertThat(() -> registry.find(AgentQuery.bySlot("reviewer", "tenant-b")),
                result -> assertThat(result).isEmpty());
    }

    @Test
    @RunOnVertxContext
    void reactive_upsert(UniAsserter asserter) {
        asserter
            .execute(() -> registry.register(descriptor("r-agent-5", "reviewer", "default", "cap-test5")))
            .execute(() -> registry.register(descriptor("r-agent-5", "planner", "default", "cap-test5b")))
            .assertThat(() -> registry.findById("r-agent-5", "default"), found -> {
                assertThat(found).isPresent();
                assertThat(found.get().slot()).isEqualTo("planner");
            });
    }
}

package io.casehub.eidos.examples;

import io.casehub.eidos.api.*;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@QuarkusTest
class TenancyIsolationTest {

    @Inject AgentRegistry registry;

    @BeforeEach
    void registerAgentsInDifferentTenants() {
        registry.register(new AgentDescriptor(
            "tenant-a-agent", "Agent A", "1.0", "anthropic",
            "claude", "claude-3-7", null, null, null, null,
            "reviewer",
            List.of(new AgentCapability("code-review", 0.9, null, null,
                List.of(), List.of(), List.of(), Map.of())),
            new AgentDisposition("collaborative", "principled", "measured", "semi-autonomous", null, false),
            null, null, "tenant-a"));

        registry.register(new AgentDescriptor(
            "tenant-b-agent", "Agent B", "1.0", "anthropic",
            "claude", "claude-3-7", null, null, null, null,
            "reviewer",
            List.of(new AgentCapability("code-review", 0.9, null, null,
                List.of(), List.of(), List.of(), Map.of())),
            new AgentDisposition("collaborative", "principled", "measured", "semi-autonomous", null, false),
            null, null, "tenant-b"));
    }

    @Test
    void tenant_a_sees_only_own_agents() {
        var agents = registry.find(AgentQuery.all("tenant-a"));
        assertThat(agents.stream().map(AgentDescriptor::agentId).toList())
            .contains("tenant-a-agent")
            .doesNotContain("tenant-b-agent");
    }

    @Test
    void tenant_b_sees_only_own_agents() {
        var agents = registry.find(AgentQuery.all("tenant-b"));
        assertThat(agents.stream().map(AgentDescriptor::agentId).toList())
            .contains("tenant-b-agent")
            .doesNotContain("tenant-a-agent");
    }

    @Test
    void find_by_id_respects_tenancy() {
        assertThat(registry.findById("tenant-a-agent", "tenant-a")).isPresent();
        assertThat(registry.findById("tenant-a-agent", "tenant-b")).isEmpty();
    }

    @Test
    void nonexistent_tenant_returns_empty() {
        assertThat(registry.find(AgentQuery.all("tenant-c"))).isEmpty();
    }
}

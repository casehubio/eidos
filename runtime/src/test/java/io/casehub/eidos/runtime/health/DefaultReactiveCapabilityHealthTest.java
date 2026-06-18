package io.casehub.eidos.runtime.health;

import io.casehub.eidos.api.*;
import io.casehub.eidos.api.CapabilityHealth.CapabilityStatus;
import io.casehub.eidos.api.CapabilityHealth.ProbeContext;
import io.casehub.eidos.runtime.registry.ReactiveTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@QuarkusTest
@TestProfile(ReactiveTestProfile.class)
class DefaultReactiveCapabilityHealthTest {

    @Inject
    ReactiveCapabilityHealth health;

    static AgentDescriptor agent(AgentCapability... capabilities) {
        return AgentDescriptor.builder()
            .agentId("agent-1")
            .name("Agent")
            .version("1.0")
            .provider("anthropic")
            .modelFamily("claude")
            .modelVersion("claude-3-7")
            .slot("reviewer")
            .capabilities(List.of(capabilities))
            .disposition(AgentDisposition.builder()
                .socialOrient("collaborative")
                .ruleFollowing("principled")
                .riskAppetite("measured")
                .autonomy("semi-autonomous")
                .build())
            .tenancyId("default")
            .build();
    }

    @Test
    void reactive_probe_returns_ready_when_capability_declared() {
        var descriptor = agent(AgentCapability.builder().name("code-review").qualityHint(0.9)
            .epistemicDomains(Map.of()).build());

        var status = health.probe(descriptor, "code-review", ProbeContext.of(null))
            .await().indefinitely();

        assertThat(status).isInstanceOf(CapabilityStatus.Ready.class);
    }

    @Test
    void reactive_probe_returns_unavailable_when_capability_missing() {
        var descriptor = agent();

        var status = health.probe(descriptor, "missing", ProbeContext.of(null))
            .await().indefinitely();

        assertThat(status).isInstanceOf(CapabilityStatus.Unavailable.class);
    }

    @Test
    void reactive_probe_returns_epistemically_weak() {
        var descriptor = agent(AgentCapability.builder().name("code-review").qualityHint(0.9)
            .epistemicDomains(Map.of("rust", 0.1)).build());

        var status = health.probe(descriptor, "code-review", ProbeContext.of("rust"))
            .await().indefinitely();

        assertThat(status).isInstanceOf(CapabilityStatus.EpistemicallyWeak.class);
    }
}

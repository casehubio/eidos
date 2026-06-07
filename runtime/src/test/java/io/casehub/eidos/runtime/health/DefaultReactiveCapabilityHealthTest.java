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
        return new AgentDescriptor(
            "agent-1", "Agent", "1.0", "anthropic", "claude", "claude-3-7",
            null, null, null, null, "reviewer",
            List.of(capabilities),
            new AgentDisposition("collaborative", "principled", "measured", "semi-autonomous", null, false),
            null, null, "default"
        );
    }

    @Test
    void reactive_probe_returns_ready_when_capability_declared() {
        var descriptor = agent(new AgentCapability("code-review", 0.9, null, null,
            List.of(), List.of(), List.of(), Map.of()));

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
        var descriptor = agent(new AgentCapability("code-review", 0.9, null, null,
            List.of(), List.of(), List.of(), Map.of("rust", 0.1)));

        var status = health.probe(descriptor, "code-review", ProbeContext.of("rust"))
            .await().indefinitely();

        assertThat(status).isInstanceOf(CapabilityStatus.EpistemicallyWeak.class);
    }
}

package io.casehub.eidos.runtime.health;

import io.casehub.eidos.api.*;
import io.casehub.eidos.api.CapabilityHealth.CapabilityStatus;
import io.casehub.eidos.api.CapabilityHealth.ProbeContext;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

@QuarkusTest
class DefaultReactiveCapabilityHealthDefaultProfileTest {

    @Inject
    ReactiveCapabilityHealth health;

    @Test
    void reactive_health_is_injectable_under_default_profile() {
        var descriptor = new AgentDescriptor(
            "agent-1", "Agent", "1.0", "anthropic", "claude", "claude-3-7",
            null, null, null, null, null, "reviewer",
            List.of(new AgentCapability("code-review", 0.9, null, null,
                List.of(), List.of(), List.of(), Map.of())),
            new AgentDisposition("collaborative", "principled", "measured", "semi-autonomous", null, false),
            null, null, "default"
        );

        var status = health.probe(descriptor, "code-review", ProbeContext.of(null))
                           .await().indefinitely();

        assertThat(status).isInstanceOf(CapabilityStatus.Ready.class);
    }
}

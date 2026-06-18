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
        var descriptor = AgentDescriptor.builder()
            .agentId("agent-1")
            .name("Agent")
            .version("1.0")
            .provider("anthropic")
            .modelFamily("claude")
            .modelVersion("claude-3-7")
            .slot("reviewer")
            .capabilities(List.of(AgentCapability.builder().name("code-review").qualityHint(0.9)
                .epistemicDomains(Map.of()).build()))
            .disposition(AgentDisposition.builder()
                .socialOrient("collaborative")
                .ruleFollowing("principled")
                .riskAppetite("measured")
                .autonomy("semi-autonomous")
                .build())
            .tenancyId("default")
            .build();

        var status = health.probe(descriptor, "code-review", ProbeContext.of(null))
                           .await().indefinitely();

        assertThat(status).isInstanceOf(CapabilityStatus.Ready.class);
    }
}

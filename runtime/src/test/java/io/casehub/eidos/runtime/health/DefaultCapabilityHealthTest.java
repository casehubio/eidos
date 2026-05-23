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
class DefaultCapabilityHealthTest {

    @Inject
    CapabilityHealth health;

    static AgentDescriptor agent(String agentId, AgentCapability... capabilities) {
        return new AgentDescriptor(
            agentId, "Agent", "1.0", "anthropic", "claude", "claude-3-7",
            null, null, null, null, "reviewer",
            List.of(capabilities),
            new AgentDisposition("collaborative", "principled", "measured", "semi-autonomous", false),
            null, null, "default"
        );
    }

    static AgentCapability capability(String name, Map<String, Double> epistemicDomains) {
        return new AgentCapability(name, 0.9, null, null,
            List.of(), List.of(), List.of(), epistemicDomains);
    }

    @Test
    void returns_ready_when_capability_declared_and_no_task_domain() {
        var descriptor = agent("a1", capability("code-review", Map.of()));
        var status = health.probe(descriptor, "code-review", ProbeContext.of(null));
        assertThat(status).isInstanceOf(CapabilityStatus.Ready.class);
    }

    @Test
    void returns_unavailable_when_capability_not_declared() {
        var descriptor = agent("a2", capability("code-review", Map.of()));
        var status = health.probe(descriptor, "test-writing", ProbeContext.of(null));
        assertThat(status).isInstanceOf(CapabilityStatus.Unavailable.class);
        assertThat(((CapabilityStatus.Unavailable) status).reason()).contains("test-writing");
    }

    @Test
    void returns_ready_when_epistemic_domain_above_threshold() {
        var descriptor = agent("a3", capability("code-review", Map.of("java", 0.95)));
        var status = health.probe(descriptor, "code-review", ProbeContext.of("java"));
        assertThat(status).isInstanceOf(CapabilityStatus.Ready.class);
    }

    @Test
    void returns_epistemically_weak_when_domain_below_threshold() {
        var descriptor = agent("a4", capability("code-review", Map.of("rust", 0.2)));
        var status = health.probe(descriptor, "code-review", ProbeContext.of("rust"));
        assertThat(status).isInstanceOf(CapabilityStatus.EpistemicallyWeak.class);
        var weak = (CapabilityStatus.EpistemicallyWeak) status;
        assertThat(weak.domain()).isEqualTo("rust");
        assertThat(weak.confidence()).isEqualTo(0.2);
    }

    @Test
    void returns_ready_when_task_domain_not_in_epistemic_map() {
        var descriptor = agent("a5", capability("code-review", Map.of("java", 0.95)));
        var status = health.probe(descriptor, "code-review", ProbeContext.of("python"));
        assertThat(status).isInstanceOf(CapabilityStatus.Ready.class);
    }

    @Test
    void returns_ready_when_epistemic_domains_null() {
        var descriptor = agent("a6", capability("code-review", null));
        var status = health.probe(descriptor, "code-review", ProbeContext.of("java"));
        assertThat(status).isInstanceOf(CapabilityStatus.Ready.class);
    }

    @Test
    void returns_ready_when_confidence_exactly_at_threshold() {
        var descriptor = agent("a7", capability("code-review", Map.of("go", 0.3)));
        var status = health.probe(descriptor, "code-review", ProbeContext.of("go"));
        assertThat(status).isInstanceOf(CapabilityStatus.Ready.class);
    }

    @Test
    void returns_unavailable_for_agent_with_no_capabilities() {
        var descriptor = agent("a8");
        var status = health.probe(descriptor, "code-review", ProbeContext.of(null));
        assertThat(status).isInstanceOf(CapabilityStatus.Unavailable.class);
    }
}

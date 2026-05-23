package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class AgentDescriptorTest {

    static AgentDescriptor minimal(String agentId, String tenancyId) {
        return new AgentDescriptor(
            agentId, "name", "1.0", "provider",
            "modelFamily", "modelVersion", null,
            null, null, null,
            "slot", List.of(),
            new AgentDisposition("collaborative", "principled", "measured", "semi-autonomous", false),
            null, null, tenancyId
        );
    }

    @Test
    void all_fields_accessible() {
        var d = minimal("agent-1", "default");
        assertThat(d.agentId()).isEqualTo("agent-1");
        assertThat(d.name()).isEqualTo("name");
        assertThat(d.tenancyId()).isEqualTo("default");
        assertThat(d.slot()).isEqualTo("slot");
        assertThat(d.capabilities()).isEmpty();
    }

    @Test
    void tenancy_id_is_last_field_and_accessible() {
        var d = minimal("x", "my-tenant");
        assertThat(d.tenancyId()).isEqualTo("my-tenant");
    }

    @Test
    void capability_fields_accessible() {
        var cap = new AgentCapability("code-review", 0.9, 500L, "low",
            List.of("java"), List.of("review"), List.of("quality"), Map.of("java", 0.95));
        assertThat(cap.name()).isEqualTo("code-review");
        assertThat(cap.qualityHint()).isEqualTo(0.9);
        assertThat(cap.latencyHintP50Ms()).isEqualTo(500L);
        assertThat(cap.epistemicDomains()).containsEntry("java", 0.95);
    }
}

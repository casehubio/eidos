package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class AgentQueryTest {

    @Test
    void null_tenancy_id_throws() {
        assertThatNullPointerException()
            .isThrownBy(() -> new AgentQuery("reviewer", null, null))
            .withMessageContaining("tenancyId");
    }

    @Test
    void bySlot_sets_correct_fields() {
        var q = AgentQuery.bySlot("reviewer", "default");
        assertThat(q.slot()).isEqualTo("reviewer");
        assertThat(q.capabilityName()).isNull();
        assertThat(q.tenancyId()).isEqualTo("default");
    }

    @Test
    void byCapability_sets_correct_fields() {
        var q = AgentQuery.byCapability("code-review", "default");
        assertThat(q.slot()).isNull();
        assertThat(q.capabilityName()).isEqualTo("code-review");
        assertThat(q.tenancyId()).isEqualTo("default");
    }

    @Test
    void bySlotAndCapability_sets_all_fields() {
        var q = AgentQuery.bySlotAndCapability("reviewer", "code-review", "default");
        assertThat(q.slot()).isEqualTo("reviewer");
        assertThat(q.capabilityName()).isEqualTo("code-review");
        assertThat(q.tenancyId()).isEqualTo("default");
    }

    @Test
    void all_sets_tenancy_only() {
        var q = AgentQuery.all("default");
        assertThat(q.slot()).isNull();
        assertThat(q.capabilityName()).isNull();
        assertThat(q.tenancyId()).isEqualTo("default");
    }
}

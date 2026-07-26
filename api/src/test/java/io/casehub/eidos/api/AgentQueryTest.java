package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class AgentQueryTest {

    @Test
    void null_tenancy_id_throws() {
        assertThatNullPointerException()
            .isThrownBy(() -> new AgentQuery("reviewer", null, null, null, null))
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
        assertThat(q.taskDomain()).isNull();
    }

    @Test
    void byCapabilityAndDomain_sets_correct_fields() {
        var q = AgentQuery.byCapabilityAndDomain("code-review", "java", "default");
        assertThat(q.slot()).isNull();
        assertThat(q.capabilityName()).isEqualTo("code-review");
        assertThat(q.taskDomain()).isEqualTo("java");
        assertThat(q.tenancyId()).isEqualTo("default");
    }

    @Test
    void byGoal_sets_goalName_and_tenancyId() {
        var q = AgentQuery.byGoal("quality-review", "t1");
        assertThat(q.goalName()).isEqualTo("quality-review");
        assertThat(q.tenancyId()).isEqualTo("t1");
        assertThat(q.slot()).isNull();
        assertThat(q.capabilityName()).isNull();
        assertThat(q.taskDomain()).isNull();
    }

}

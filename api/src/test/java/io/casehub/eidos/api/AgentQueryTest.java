package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class AgentQueryTest {

    @Test
    void null_tenancy_id_throws() {
        assertThatNullPointerException()
                .isThrownBy(() -> new AgentQuery("reviewer", null, null, null, null, null))
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


    @Test
    void byProximity_creates_query_with_maxDepth() {
        var query = AgentQuery.byProximity("code-review", 2, "tenant-a");
        assertThat(query.capabilityName()).isEqualTo("code-review");
        assertThat(query.maxDepth()).isEqualTo(2);
        assertThat(query.tenancyId()).isEqualTo("tenant-a");
        assertThat(query.slot()).isNull();
        assertThat(query.taskDomain()).isNull();
        assertThat(query.goalName()).isNull();
    }

    @Test
    void byProximityAndDomain_creates_query_with_maxDepth_and_domain() {
        var query = AgentQuery.byProximityAndDomain("code-review", 3, "java", "tenant-a");
        assertThat(query.capabilityName()).isEqualTo("code-review");
        assertThat(query.maxDepth()).isEqualTo(3);
        assertThat(query.taskDomain()).isEqualTo("java");
        assertThat(query.tenancyId()).isEqualTo("tenant-a");
    }

    @Test
    void byProximity_rejects_zero_maxDepth() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> AgentQuery.byProximity("code-review", 0, "tenant-a"))
                .withMessageContaining("maxDepth");
    }

    @Test
    void byProximity_rejects_negative_maxDepth() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> AgentQuery.byProximity("code-review", -1, "tenant-a"))
                .withMessageContaining("maxDepth");
    }

    @Test
    void existing_factories_have_null_maxDepth() {
        assertThat(AgentQuery.bySlot("reviewer", "t").maxDepth()).isNull();
        assertThat(AgentQuery.byCapability("code-review", "t").maxDepth()).isNull();
        assertThat(AgentQuery.all("t").maxDepth()).isNull();
        assertThat(AgentQuery.byGoal("quality", "t").maxDepth()).isNull();
        assertThat(AgentQuery.byCapabilityAndDomain("cr", "java", "t").maxDepth()).isNull();
        assertThat(AgentQuery.bySlotAndCapability("r", "cr", "t").maxDepth()).isNull();
    }
}

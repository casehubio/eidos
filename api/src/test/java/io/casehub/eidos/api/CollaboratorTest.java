package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class CollaboratorTest {

    @Test
    void valid_collaborator() {
        var c = new Collaborator("agent-1", "tenant-a",
            Set.of(CollaborationRelation.COACTIVE), 0.8);
        assertThat(c.agentId()).isEqualTo("agent-1");
        assertThat(c.tenancyId()).isEqualTo("tenant-a");
        assertThat(c.relations()).containsExactly(CollaborationRelation.COACTIVE);
        assertThat(c.affinity()).isEqualTo(0.8);
    }

    @Test
    void relations_are_immutable() {
        var c = new Collaborator("a", "t",
            Set.of(CollaborationRelation.COACTIVE, CollaborationRelation.COMPLEMENTARY), 0.5);
        assertThatThrownBy(() -> c.relations().add(CollaborationRelation.SHARED_INTEREST))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejects_null_agentId() {
        assertThatNullPointerException()
            .isThrownBy(() -> new Collaborator(null, "t", Set.of(), 0.5));
    }

    @Test
    void rejects_null_tenancyId() {
        assertThatNullPointerException()
            .isThrownBy(() -> new Collaborator("a", null, Set.of(), 0.5));
    }

    @Test
    void rejects_affinity_below_zero() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new Collaborator("a", "t", Set.of(), -0.1))
            .withMessageContaining("affinity");
    }

    @Test
    void rejects_affinity_above_one() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new Collaborator("a", "t", Set.of(), 1.1))
            .withMessageContaining("affinity");
    }

    @Test
    void boundary_affinity_values_accepted() {
        assertThatNoException()
            .isThrownBy(() -> new Collaborator("a", "t", Set.of(), 0.0));
        assertThatNoException()
            .isThrownBy(() -> new Collaborator("a", "t", Set.of(), 1.0));
    }
}

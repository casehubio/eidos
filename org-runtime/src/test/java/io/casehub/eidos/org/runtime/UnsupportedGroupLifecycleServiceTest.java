package io.casehub.eidos.org.runtime;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UnsupportedGroupLifecycleServiceTest {

    private final UnsupportedGroupLifecycleService service = new UnsupportedGroupLifecycleService();

    @Test void createGroupThrows() {
        assertThatThrownBy(() -> service.createGroup("spec", "t1", null))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("No GroupLifecycleService configured");
    }

    @Test void proposeGroupThrows() {
        assertThatThrownBy(() -> service.proposeGroup("spec", "t1", "agent"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test void approveProposalThrows() {
        assertThatThrownBy(() -> service.approveProposal("id", "t1"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test void rejectProposalThrows() {
        assertThatThrownBy(() -> service.rejectProposal("id", "t1", "reason"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test void activateGroupThrows() {
        assertThatThrownBy(() -> service.activateGroup("id", "t1"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test void dissolveGroupThrows() {
        assertThatThrownBy(() -> service.dissolveGroup("id", "t1", "reason"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test void changeTopologyThrows() {
        assertThatThrownBy(() -> service.changeTopology("id", "mesh", null, "t1"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test void joinGroupThrows() {
        assertThatThrownBy(() -> service.joinGroup("id", "agent", "t1"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test void leaveGroupThrows() {
        assertThatThrownBy(() -> service.leaveGroup("id", "agent", "t1"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test void adoptRoleThrows() {
        assertThatThrownBy(() -> service.adoptRole("id", "agent", "role", "t1"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test void releaseRoleThrows() {
        assertThatThrownBy(() -> service.releaseRole("id", "agent", "role", "t1"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test void commitToMissionThrows() {
        assertThatThrownBy(() -> service.commitToMission("id", "agent", "mission", "t1"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test void withdrawFromMissionThrows() {
        assertThatThrownBy(() -> service.withdrawFromMission("id", "agent", "mission", "t1"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test void fulfillMissionThrows() {
        assertThatThrownBy(() -> service.fulfillMission("id", "mission", "t1"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test void findInstanceThrows() {
        assertThatThrownBy(() -> service.findInstance("id", "t1"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test void findInstancesThrows() {
        assertThatThrownBy(() -> service.findInstances(null))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test void instancesForAgentThrows() {
        assertThatThrownBy(() -> service.instancesForAgent("agent", "t1"))
            .isInstanceOf(UnsupportedOperationException.class);
    }
}

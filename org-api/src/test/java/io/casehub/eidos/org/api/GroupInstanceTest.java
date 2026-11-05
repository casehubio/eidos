package io.casehub.eidos.org.api;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GroupInstanceTest {

    @Test void minimalGroupInstance() {
        var gi = new GroupInstance("gi-1", "spec-1", "t1",
            GroupLifecycleState.FORMING, FormationOrigin.TOP_DOWN,
            null, null, null, List.of(), List.of(), List.of(),
            Instant.now(), Instant.now(), 0);
        assertThat(gi.instanceId()).isEqualTo("gi-1");
        assertThat(gi.state()).isEqualTo(GroupLifecycleState.FORMING);
        assertThat(gi.members()).isEmpty();
        assertThat(gi.roleAdoptions()).isEmpty();
        assertThat(gi.version()).isZero();
    }

    @Test void requiresInstanceId() {
        assertThatThrownBy(() -> new GroupInstance(null, "s", "t",
            GroupLifecycleState.FORMING, FormationOrigin.TOP_DOWN,
            null, null, null, List.of(), List.of(), List.of(),
            Instant.now(), Instant.now(), 0))
            .isInstanceOf(NullPointerException.class);
    }

    @Test void requiresState() {
        assertThatThrownBy(() -> new GroupInstance("gi", "s", "t",
            null, FormationOrigin.TOP_DOWN,
            null, null, null, List.of(), List.of(), List.of(),
            Instant.now(), Instant.now(), 0))
            .isInstanceOf(NullPointerException.class);
    }

    @Test void rejectsNegativeVersion() {
        assertThatThrownBy(() -> new GroupInstance("gi", "s", "t",
            GroupLifecycleState.FORMING, FormationOrigin.TOP_DOWN,
            null, null, null, List.of(), List.of(), List.of(),
            Instant.now(), Instant.now(), -1))
            .isInstanceOf(OrgValidationException.class)
            .hasMessageContaining("version");
    }

    @Test void membershipInvariantEnforced() {
        var member = new ActualMembership("agent-1", Instant.now());
        var adoption = new RoleAdoption("agent-2", "reviewer", null, Instant.now());
        assertThatThrownBy(() -> new GroupInstance("gi", "s", "t",
            GroupLifecycleState.ACTIVE, FormationOrigin.TOP_DOWN,
            null, null, null, List.of(member), List.of(adoption), List.of(),
            Instant.now(), Instant.now(), 0))
            .isInstanceOf(OrgValidationException.class)
            .hasMessageContaining("membership");
    }

    @Test void membershipInvariantPasses() {
        var member = new ActualMembership("agent-1", Instant.now());
        var adoption = new RoleAdoption("agent-1", "reviewer", null, Instant.now());
        var gi = new GroupInstance("gi", "s", "t",
            GroupLifecycleState.ACTIVE, FormationOrigin.TOP_DOWN,
            null, null, null, List.of(member), List.of(adoption), List.of(),
            Instant.now(), Instant.now(), 0);
        assertThat(gi.roleAdoptions()).hasSize(1);
    }

    @Test void defensiveCopyOfMembers() {
        var member = new ActualMembership("a1", Instant.now());
        var mutable = new ArrayList<>(List.of(member));
        var gi = new GroupInstance("gi", "s", "t",
            GroupLifecycleState.ACTIVE, FormationOrigin.TOP_DOWN,
            null, null, null, mutable, List.of(), List.of(),
            Instant.now(), Instant.now(), 0);
        mutable.clear();
        assertThat(gi.members()).hasSize(1);
    }

    @Test void defensiveCopyOfAdoptions() {
        var member = new ActualMembership("a1", Instant.now());
        var adoption = new RoleAdoption("a1", "reviewer", null, Instant.now());
        var mutable = new ArrayList<>(List.of(adoption));
        var gi = new GroupInstance("gi", "s", "t",
            GroupLifecycleState.ACTIVE, FormationOrigin.TOP_DOWN,
            null, null, null, List.of(member), mutable, List.of(),
            Instant.now(), Instant.now(), 0);
        mutable.clear();
        assertThat(gi.roleAdoptions()).hasSize(1);
    }

    @Test void lifecycleStateTerminalCheck() {
        assertThat(GroupLifecycleState.DISSOLVED.isTerminal()).isTrue();
        assertThat(GroupLifecycleState.REJECTED.isTerminal()).isTrue();
        assertThat(GroupLifecycleState.FAILED.isTerminal()).isTrue();
        assertThat(GroupLifecycleState.ACTIVE.isTerminal()).isFalse();
        assertThat(GroupLifecycleState.FORMING.isTerminal()).isFalse();
        assertThat(GroupLifecycleState.PROPOSED.isTerminal()).isFalse();
    }

    @Test void withTopologyAndAuthority() {
        var gi = new GroupInstance("gi", "s", "t",
            GroupLifecycleState.ACTIVE, FormationOrigin.TOP_DOWN,
            "mesh", "urn:topo", AuthorityModel.DISTRIBUTED,
            List.of(), List.of(), List.of(),
            Instant.now(), Instant.now(), 0);
        assertThat(gi.topology()).isEqualTo("mesh");
        assertThat(gi.effectiveAuthority()).isEqualTo(AuthorityModel.DISTRIBUTED);
    }

    @Test
    void withState() {
        var now = Instant.now();
        var gi = new GroupInstance("gi", "s", "t",
                                   GroupLifecycleState.FORMING, FormationOrigin.TOP_DOWN,
                                   null, null, null, List.of(), List.of(), List.of(),
                                   now, now, 0);
        var updated = gi.withState(GroupLifecycleState.ACTIVE);
        assertThat(updated.state()).isEqualTo(GroupLifecycleState.ACTIVE);
        assertThat(updated.instanceId()).isEqualTo("gi");
        assertThat(updated.version()).isZero();
    }

    @Test
    void withTopology() {
        var now = Instant.now();
        var gi = new GroupInstance("gi", "s", "t",
                                   GroupLifecycleState.ACTIVE, FormationOrigin.TOP_DOWN,
                                   "swarm", null, null, List.of(), List.of(), List.of(),
                                   now, now, 0);
        var updated = gi.withTopology("hive", "urn:topo");
        assertThat(updated.topology()).isEqualTo("hive");
        assertThat(updated.topologyVocabulary()).isEqualTo("urn:topo");
        assertThat(updated.state()).isEqualTo(GroupLifecycleState.ACTIVE);
    }

    @Test
    void withEffectiveAuthority() {
        var now = Instant.now();
        var gi = new GroupInstance("gi", "s", "t",
                                   GroupLifecycleState.ACTIVE, FormationOrigin.TOP_DOWN,
                                   null, null, AuthorityModel.DISTRIBUTED, List.of(), List.of(), List.of(),
                                   now, now, 0);
        var updated = gi.withEffectiveAuthority(AuthorityModel.CENTRALISED);
        assertThat(updated.effectiveAuthority()).isEqualTo(AuthorityModel.CENTRALISED);
    }

    @Test
    void withMembers() {
        var now = Instant.now();
        var gi = new GroupInstance("gi", "s", "t",
                                   GroupLifecycleState.ACTIVE, FormationOrigin.TOP_DOWN,
                                   null, null, null, List.of(), List.of(), List.of(),
                                   now, now, 0);
        var member  = new ActualMembership("agent-1", now);
        var updated = gi.withMembers(List.of(member));
        assertThat(updated.members()).containsExactly(member);
        assertThat(updated.instanceId()).isEqualTo("gi");
    }

    @Test
    void withRoleAdoptions() {
        var now    = Instant.now();
        var member = new ActualMembership("agent-1", now);
        var gi = new GroupInstance("gi", "s", "t",
                                   GroupLifecycleState.ACTIVE, FormationOrigin.TOP_DOWN,
                                   null, null, null, List.of(member), List.of(), List.of(),
                                   now, now, 0);
        var adoption = new RoleAdoption("agent-1", "lead", null, now);
        var updated  = gi.withRoleAdoptions(List.of(adoption));
        assertThat(updated.roleAdoptions()).containsExactly(adoption);
    }

    @Test
    void withMissionCommitments() {
        var now = Instant.now();
        var gi = new GroupInstance("gi", "s", "t",
                                   GroupLifecycleState.ACTIVE, FormationOrigin.TOP_DOWN,
                                   null, null, null, List.of(), List.of(), List.of(),
                                   now, now, 0);
        var commitment = new MissionCommitment("mission-1", "agent-1", now);
        var updated    = gi.withMissionCommitments(List.of(commitment));
        assertThat(updated.missionCommitments()).containsExactly(commitment);
    }

    @Test
    void withLastTransitionAt() {
        var now   = Instant.now();
        var later = now.plusSeconds(60);
        var gi = new GroupInstance("gi", "s", "t",
                                   GroupLifecycleState.ACTIVE, FormationOrigin.TOP_DOWN,
                                   null, null, null, List.of(), List.of(), List.of(),
                                   now, now, 0);
        var updated = gi.withLastTransitionAt(later);
        assertThat(updated.lastTransitionAt()).isEqualTo(later);
        assertThat(updated.createdAt()).isEqualTo(now);
    }

    @Test
    void withVersion() {
        var now = Instant.now();
        var gi = new GroupInstance("gi", "s", "t",
                                   GroupLifecycleState.ACTIVE, FormationOrigin.TOP_DOWN,
                                   null, null, null, List.of(), List.of(), List.of(),
                                   now, now, 0);
        var updated = gi.withVersion(5);
        assertThat(updated.version()).isEqualTo(5);
    }

    @Test
    void witherValidatesRoleAdoptionMembershipInvariant() {
        var now = Instant.now();
        var gi = new GroupInstance("gi", "s", "t",
                                   GroupLifecycleState.ACTIVE, FormationOrigin.TOP_DOWN,
                                   null, null, null, List.of(), List.of(), List.of(),
                                   now, now, 0);
        var adoption = new RoleAdoption("nonexistent", "lead", null, now);
        assertThatThrownBy(() -> gi.withRoleAdoptions(List.of(adoption)))
                .isInstanceOf(OrgValidationException.class)
                .hasMessageContaining("membership");
    }

}

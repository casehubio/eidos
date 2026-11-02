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
}

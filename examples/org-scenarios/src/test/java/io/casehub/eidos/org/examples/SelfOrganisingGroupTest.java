package io.casehub.eidos.org.examples;

import io.casehub.eidos.org.api.*;
import io.casehub.eidos.org.memory.InMemoryGroupInstanceStore;
import io.casehub.eidos.org.memory.InMemoryOrgRegistry;
import io.casehub.eidos.org.runtime.DefaultGroupLifecycleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SelfOrganisingGroupTest {

    private GroupLifecycleService lifecycleService;
    private InMemoryOrgRegistry orgRegistry;

    @BeforeEach void registerSpec() {
        var store = new InMemoryGroupInstanceStore();
        orgRegistry = new InMemoryOrgRegistry();
        lifecycleService = new DefaultGroupLifecycleService(store, orgRegistry, List.of());

        var reviewerRole = new RoleSpec("reviewer", null, 1, 3, Set.of(), List.of());
        var authorRole = new RoleSpec("author", null, 1, 1, Set.of("auditor"), List.of());
        var auditorRole = new RoleSpec("auditor", null, 0, 1, Set.of("author"), List.of());

        var unit = OrganizationalUnit.builder()
            .unitId("review-team").name("Review Team").tenancyId("t1")
            .topology("hierarchy")
            .authorityModel(AuthorityModel.CENTRALISED)
            .selfDetermination(0.3)
            .roleSpecs(List.of(reviewerRole, authorRole, auditorRole))
            .build();
        orgRegistry.registerUnit(unit);
    }

    @Test void fullLifecycle() {
        var group = lifecycleService.createGroup("review-team", "t1", FormationOrigin.TOP_DOWN);
        assertThat(group.state()).isEqualTo(GroupLifecycleState.FORMING);

        lifecycleService.joinGroup(group.instanceId(), "alice", "t1");
        lifecycleService.joinGroup(group.instanceId(), "bob", "t1");
        lifecycleService.joinGroup(group.instanceId(), "charlie", "t1");

        lifecycleService.adoptRole(group.instanceId(), "alice", "reviewer", "t1");
        lifecycleService.adoptRole(group.instanceId(), "bob", "author", "t1");

        var updated = lifecycleService.findInstance(group.instanceId(), "t1").orElseThrow();
        assertThat(updated.roleAdoptions()).hasSize(2);
        assertThat(updated.members()).hasSize(3);

        assertThatThrownBy(() ->
            lifecycleService.adoptRole(group.instanceId(), "bob", "auditor", "t1"))
            .isInstanceOf(OrgValidationException.class)
            .hasMessageContaining("incompatible");

        assertThatThrownBy(() ->
            lifecycleService.adoptRole(group.instanceId(), "charlie", "author", "t1"))
            .isInstanceOf(OrgValidationException.class)
            .hasMessageContaining("cardinality");

        lifecycleService.activateGroup(group.instanceId(), "t1");
        updated = lifecycleService.findInstance(group.instanceId(), "t1").orElseThrow();
        assertThat(updated.state()).isEqualTo(GroupLifecycleState.ACTIVE);

        lifecycleService.changeTopology(group.instanceId(), "mesh", null, "t1");
        updated = lifecycleService.findInstance(group.instanceId(), "t1").orElseThrow();
        assertThat(updated.topology()).isEqualTo("mesh");

        lifecycleService.releaseRole(group.instanceId(), "alice", "reviewer", "t1");

        lifecycleService.dissolveGroup(group.instanceId(), "t1", "done");
        updated = lifecycleService.findInstance(group.instanceId(), "t1").orElseThrow();
        assertThat(updated.state()).isEqualTo(GroupLifecycleState.DISSOLVING);

        lifecycleService.leaveGroup(group.instanceId(), "alice", "t1");
        lifecycleService.leaveGroup(group.instanceId(), "bob", "t1");
        lifecycleService.leaveGroup(group.instanceId(), "charlie", "t1");

        updated = lifecycleService.findInstance(group.instanceId(), "t1").orElseThrow();
        assertThat(updated.state()).isEqualTo(GroupLifecycleState.DISSOLVED);
    }

    @Test void proposalFlow() {
        var proposed = lifecycleService.proposeGroup("review-team", "t1", "alice");
        assertThat(proposed.state()).isEqualTo(GroupLifecycleState.PROPOSED);

        lifecycleService.approveProposal(proposed.instanceId(), "t1");
        var forming = lifecycleService.findInstance(proposed.instanceId(), "t1").orElseThrow();
        assertThat(forming.state()).isEqualTo(GroupLifecycleState.FORMING);
    }

    @Test void rejectedProposal() {
        var proposed = lifecycleService.proposeGroup("review-team", "t1", "alice");
        lifecycleService.rejectProposal(proposed.instanceId(), "t1", "not approved");
        var rejected = lifecycleService.findInstance(proposed.instanceId(), "t1").orElseThrow();
        assertThat(rejected.state()).isEqualTo(GroupLifecycleState.REJECTED);
        assertThat(rejected.state().isTerminal()).isTrue();
    }

    @Test void formationRulesRestrictPaths() {
        var rules = new FormationRules(Set.of(FormationPath.TOP_DOWN), 2, 10, false, 1.0);
        var policy = new GroupPolicy(rules, 0, null);
        var restrictedUnit = OrganizationalUnit.builder()
            .unitId("restricted-team").name("Restricted").tenancyId("t1")
            .policy(policy).build();
        orgRegistry.registerUnit(restrictedUnit);

        var gi = lifecycleService.createGroup("restricted-team", "t1", FormationOrigin.TOP_DOWN);
        assertThat(gi.state()).isEqualTo(GroupLifecycleState.FORMING);

        assertThatThrownBy(() ->
            lifecycleService.proposeGroup("restricted-team", "t1", "alice"))
            .isInstanceOf(OrgValidationException.class)
            .hasMessageContaining("formation");
    }

    @Test void membershipInvariantPreventsOrphanRoleAdoption() {
        var group = lifecycleService.createGroup("review-team", "t1", FormationOrigin.TOP_DOWN);

        assertThatThrownBy(() ->
            lifecycleService.adoptRole(group.instanceId(), "alice", "reviewer", "t1"))
            .isInstanceOf(OrgValidationException.class)
            .hasMessageContaining("member");

        lifecycleService.joinGroup(group.instanceId(), "alice", "t1");
        lifecycleService.adoptRole(group.instanceId(), "alice", "reviewer", "t1");

        var updated = lifecycleService.findInstance(group.instanceId(), "t1").orElseThrow();
        assertThat(updated.members()).hasSize(1);
        assertThat(updated.roleAdoptions()).hasSize(1);
    }

    @Test void specWithAuthorityAndSelfDetermination() {
        var group = lifecycleService.createGroup("review-team", "t1", FormationOrigin.TOP_DOWN);
        assertThat(group.effectiveAuthority()).isEqualTo(AuthorityModel.CENTRALISED);

        var spec = orgRegistry.findUnit("review-team", "t1").orElseThrow();
        assertThat(spec.selfDetermination()).isEqualTo(0.3);
        assertThat(spec.authorityModel()).isEqualTo(AuthorityModel.CENTRALISED);
    }
}

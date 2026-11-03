package io.casehub.eidos.org.runtime;

import io.casehub.eidos.org.api.*;
import io.casehub.eidos.org.api.spi.GroupInstanceStore;
import io.casehub.eidos.org.memory.InMemoryGroupInstanceStore;
import io.casehub.eidos.org.memory.InMemoryOrgRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultGroupLifecycleServiceTest {

    private GroupInstanceStore store;
    private InMemoryOrgRegistry orgRegistry;
    private GroupLifecycleService service;
    private List<String> events;

    @BeforeEach void setUp() {
        store = new InMemoryGroupInstanceStore();
        orgRegistry = new InMemoryOrgRegistry();
        events = new ArrayList<>();

        var listener = new GroupLifecycleListener() {
            @Override public void onFormationComplete(String id, String t) { events.add("formed:" + id); }
            @Override public void onMemberJoined(String id, String a, String t) { events.add("join:" + a); }
            @Override public void onMemberLeft(String id, String a, String t) { events.add("left:" + a); }
            @Override public void onRoleAdoption(String id, String a, String r, String t) { events.add("adopt:" + a + ":" + r); }
            @Override public void onRoleRelease(String id, String a, String r, String t) { events.add("release:" + a + ":" + r); }
            @Override public void onDissolution(String id, String t, String reason) { events.add("dissolve:" + id); }
            @Override public void onTopologyChange(String id, String o, String n, String t) { events.add("topology:" + o + "->" + n); }
            @Override public void onStateTransition(String id, GroupLifecycleState from, GroupLifecycleState to, String t) { events.add("state:" + from + "->" + to); }
        };

        service = new DefaultGroupLifecycleService(store, orgRegistry, List.of(listener));
    }

    private void registerSpec(String unitId, List<RoleSpec> roleSpecs) {
        var unit = OrganizationalUnit.builder()
            .unitId(unitId).name("Test").tenancyId("t1")
            .roleSpecs(roleSpecs).build();
        orgRegistry.registerUnit(unit);
    }

    private void registerSpecWithPolicy(String unitId, List<RoleSpec> roleSpecs, GroupPolicy policy) {
        var unit = OrganizationalUnit.builder()
            .unitId(unitId).name("Test").tenancyId("t1")
            .roleSpecs(roleSpecs).policy(policy).build();
        orgRegistry.registerUnit(unit);
    }

    private void registerSpecWithMissions(String unitId, List<RoleSpec> roleSpecs, List<MissionSpec> missions) {
        var unit = OrganizationalUnit.builder()
            .unitId(unitId).name("Test").tenancyId("t1")
            .roleSpecs(roleSpecs).missions(missions).build();
        orgRegistry.registerUnit(unit);
    }

    @Test void createGroupTopDown() {
        registerSpec("spec-1", List.of(new RoleSpec("reviewer", null, 0, 3, Set.of(), List.of())));
        var gi = service.createGroup("spec-1", "t1", FormationOrigin.TOP_DOWN);
        assertThat(gi.state()).isEqualTo(GroupLifecycleState.FORMING);
        assertThat(gi.formationOrigin()).isEqualTo(FormationOrigin.TOP_DOWN);
        assertThat(gi.specId()).isEqualTo("spec-1");
    }

    @Test void proposeGroupStartsProposed() {
        registerSpec("spec-1", List.of());
        var gi = service.proposeGroup("spec-1", "t1", "agent-1");
        assertThat(gi.state()).isEqualTo(GroupLifecycleState.PROPOSED);
    }

    @Test void approveProposalTransitionsToForming() {
        registerSpec("spec-1", List.of());
        var gi = service.proposeGroup("spec-1", "t1", "agent-1");
        service.approveProposal(gi.instanceId(), "t1");
        var updated = service.findInstance(gi.instanceId(), "t1").orElseThrow();
        assertThat(updated.state()).isEqualTo(GroupLifecycleState.FORMING);
    }

    @Test void rejectProposalTransitionsToRejected() {
        registerSpec("spec-1", List.of());
        var gi = service.proposeGroup("spec-1", "t1", "agent-1");
        service.rejectProposal(gi.instanceId(), "t1", "not needed");
        var updated = service.findInstance(gi.instanceId(), "t1").orElseThrow();
        assertThat(updated.state()).isEqualTo(GroupLifecycleState.REJECTED);
    }

    @Test void joinGroupBeforeAdoptRole() {
        registerSpec("spec-1", List.of(new RoleSpec("reviewer", null, 0, 1, Set.of(), List.of())));
        var gi = service.createGroup("spec-1", "t1", FormationOrigin.TOP_DOWN);
        assertThatThrownBy(() -> service.adoptRole(gi.instanceId(), "agent-1", "reviewer", "t1"))
            .isInstanceOf(OrgValidationException.class)
            .hasMessageContaining("member");
        service.joinGroup(gi.instanceId(), "agent-1", "t1");
        service.adoptRole(gi.instanceId(), "agent-1", "reviewer", "t1");
        assertThat(events).contains("adopt:agent-1:reviewer");
    }

    @Test void leaveGroupReleasesAllRoles() {
        registerSpec("spec-1", List.of(new RoleSpec("reviewer", null, 0, -1, Set.of(), List.of())));
        var gi = service.createGroup("spec-1", "t1", FormationOrigin.TOP_DOWN);
        service.joinGroup(gi.instanceId(), "agent-1", "t1");
        service.adoptRole(gi.instanceId(), "agent-1", "reviewer", "t1");
        service.leaveGroup(gi.instanceId(), "agent-1", "t1");
        var updated = service.findInstance(gi.instanceId(), "t1").orElseThrow();
        assertThat(updated.members()).isEmpty();
        assertThat(updated.roleAdoptions()).isEmpty();
    }

    @Test void adoptRoleEnforcesMaxCardinality() {
        registerSpec("spec-1", List.of(new RoleSpec("reviewer", null, 0, 1, Set.of(), List.of())));
        var gi = service.createGroup("spec-1", "t1", FormationOrigin.TOP_DOWN);
        service.joinGroup(gi.instanceId(), "agent-1", "t1");
        service.joinGroup(gi.instanceId(), "agent-2", "t1");
        service.adoptRole(gi.instanceId(), "agent-1", "reviewer", "t1");
        assertThatThrownBy(() -> service.adoptRole(gi.instanceId(), "agent-2", "reviewer", "t1"))
            .isInstanceOf(OrgValidationException.class)
            .hasMessageContaining("cardinality");
    }

    @Test void adoptRoleEnforcesIncompatibility() {
        registerSpec("spec-1", List.of(
            new RoleSpec("auditor", null, 0, -1, Set.of("developer"), List.of()),
            new RoleSpec("developer", null, 0, -1, Set.of("auditor"), List.of())
        ));
        var gi = service.createGroup("spec-1", "t1", FormationOrigin.TOP_DOWN);
        service.joinGroup(gi.instanceId(), "agent-1", "t1");
        service.adoptRole(gi.instanceId(), "agent-1", "developer", "t1");
        assertThatThrownBy(() -> service.adoptRole(gi.instanceId(), "agent-1", "auditor", "t1"))
            .isInstanceOf(OrgValidationException.class)
            .hasMessageContaining("incompatible");
    }

    @Test void releaseRoleSucceedsEvenBelowMinCardinality() {
        registerSpec("spec-1", List.of(new RoleSpec("reviewer", null, 2, -1, Set.of(), List.of())));
        var gi = service.createGroup("spec-1", "t1", FormationOrigin.TOP_DOWN);
        service.joinGroup(gi.instanceId(), "agent-1", "t1");
        service.adoptRole(gi.instanceId(), "agent-1", "reviewer", "t1");
        service.releaseRole(gi.instanceId(), "agent-1", "reviewer", "t1");
        var updated = service.findInstance(gi.instanceId(), "t1").orElseThrow();
        assertThat(updated.roleAdoptions()).isEmpty();
        assertThat(events).contains("release:agent-1:reviewer");
    }

    @Test void changeTopologyOnFormingGroupDirectSwap() {
        registerSpec("spec-1", List.of());
        var gi = service.createGroup("spec-1", "t1", FormationOrigin.TOP_DOWN);
        events.clear();
        service.changeTopology(gi.instanceId(), "mesh", null, "t1");
        var updated = service.findInstance(gi.instanceId(), "t1").orElseThrow();
        assertThat(updated.topology()).isEqualTo("mesh");
        assertThat(updated.state()).isEqualTo(GroupLifecycleState.FORMING);
        assertThat(events).noneMatch(e -> e.contains("REORGANISING"));
        assertThat(events).anyMatch(e -> e.contains("topology:"));
    }

    @Test void changeTopologyOnActiveGroupGoesViaReorganising() {
        registerSpec("spec-1", List.of());
        var gi = service.createGroup("spec-1", "t1", FormationOrigin.TOP_DOWN);
        service.activateGroup(gi.instanceId(), "t1");
        events.clear();

        service.changeTopology(gi.instanceId(), "ring", "urn:vocab:topo", "t1");

        var updated = service.findInstance(gi.instanceId(), "t1").orElseThrow();
        assertThat(updated.topology()).isEqualTo("ring");
        assertThat(updated.topologyVocabulary()).isEqualTo("urn:vocab:topo");
        assertThat(updated.state()).isEqualTo(GroupLifecycleState.ACTIVE);
        assertThat(events).containsSubsequence(
            "state:ACTIVE->REORGANISING",
            "state:REORGANISING->ACTIVE",
            "topology:null->ring"
        );
    }

    @Test void roleAdoptionBlockedDuringReorganising() {
        registerSpec("spec-1", List.of(new RoleSpec("reviewer", null, 0, -1, Set.of(), List.of())));
        var gi = service.createGroup("spec-1", "t1", FormationOrigin.TOP_DOWN);
        service.joinGroup(gi.instanceId(), "agent-1", "t1");
        service.activateGroup(gi.instanceId(), "t1");

        var current = service.findInstance(gi.instanceId(), "t1").orElseThrow();
        store.save(new GroupInstance(
            current.instanceId(), current.specId(), current.tenancyId(),
            GroupLifecycleState.REORGANISING, current.formationOrigin(),
            current.topology(), current.topologyVocabulary(), current.effectiveAuthority(),
            current.members(), current.roleAdoptions(), current.missionCommitments(),
            current.createdAt(), current.lastTransitionAt(), current.version()
        ));

        assertThatThrownBy(() -> service.adoptRole(gi.instanceId(), "agent-1", "reviewer", "t1"))
            .isInstanceOf(OrgValidationException.class)
            .hasMessageContaining("REORGANISING");
    }

    @Test void dissolveEmptyGroupAutoCompletes() {
        registerSpec("spec-1", List.of());
        var gi = service.createGroup("spec-1", "t1", FormationOrigin.TOP_DOWN);
        service.dissolveGroup(gi.instanceId(), "t1", "done");
        var updated = service.findInstance(gi.instanceId(), "t1").orElseThrow();
        assertThat(updated.state()).isEqualTo(GroupLifecycleState.DISSOLVED);
    }

    @Test void dissolveGroupWithMembersEntersDissolving() {
        registerSpec("spec-1", List.of(new RoleSpec("reviewer", null, 0, -1, Set.of(), List.of())));
        var gi = service.createGroup("spec-1", "t1", FormationOrigin.TOP_DOWN);
        service.joinGroup(gi.instanceId(), "agent-1", "t1");
        service.dissolveGroup(gi.instanceId(), "t1", "done");
        var updated = service.findInstance(gi.instanceId(), "t1").orElseThrow();
        assertThat(updated.state()).isEqualTo(GroupLifecycleState.DISSOLVING);
        service.leaveGroup(gi.instanceId(), "agent-1", "t1");
        updated = service.findInstance(gi.instanceId(), "t1").orElseThrow();
        assertThat(updated.state()).isEqualTo(GroupLifecycleState.DISSOLVED);
    }

    @Test void stateTransitionValidation() {
        registerSpec("spec-1", List.of());
        var gi = service.createGroup("spec-1", "t1", FormationOrigin.TOP_DOWN);
        service.dissolveGroup(gi.instanceId(), "t1", "done");
        assertThatThrownBy(() -> service.joinGroup(gi.instanceId(), "agent-1", "t1"))
            .isInstanceOf(OrgValidationException.class);
    }

    @Test void formationRulesEnforced() {
        var rules = new FormationRules(Set.of(FormationPath.TOP_DOWN), 2, 10, false, 1.0);
        registerSpecWithPolicy("spec-1", List.of(), new GroupPolicy(rules, 0, null));
        assertThatThrownBy(() -> service.proposeGroup("spec-1", "t1", "agent-1"))
            .isInstanceOf(OrgValidationException.class)
            .hasMessageContaining("formation");
    }

    @Test void formationRulesAllowTopDown() {
        var rules = new FormationRules(Set.of(FormationPath.TOP_DOWN), 2, 10, false, 1.0);
        registerSpecWithPolicy("spec-1", List.of(), new GroupPolicy(rules, 0, null));
        var gi = service.createGroup("spec-1", "t1", FormationOrigin.TOP_DOWN);
        assertThat(gi.state()).isEqualTo(GroupLifecycleState.FORMING);
    }

    @Test void activateGroupTransitionsToActive() {
        registerSpec("spec-1", List.of(new RoleSpec("reviewer", null, 1, 3, Set.of(), List.of())));
        var gi = service.createGroup("spec-1", "t1", FormationOrigin.TOP_DOWN);
        service.joinGroup(gi.instanceId(), "agent-1", "t1");
        service.adoptRole(gi.instanceId(), "agent-1", "reviewer", "t1");
        events.clear();

        service.activateGroup(gi.instanceId(), "t1");

        var updated = service.findInstance(gi.instanceId(), "t1").orElseThrow();
        assertThat(updated.state()).isEqualTo(GroupLifecycleState.ACTIVE);
        assertThat(events).contains("state:FORMING->ACTIVE", "formed:" + gi.instanceId());
    }

    @Test void activateGroupRejectsInsufficientRoles() {
        registerSpec("spec-1", List.of(new RoleSpec("reviewer", null, 2, 3, Set.of(), List.of())));
        var gi = service.createGroup("spec-1", "t1", FormationOrigin.TOP_DOWN);
        service.joinGroup(gi.instanceId(), "agent-1", "t1");
        service.adoptRole(gi.instanceId(), "agent-1", "reviewer", "t1");

        assertThatThrownBy(() -> service.activateGroup(gi.instanceId(), "t1"))
            .isInstanceOf(OrgValidationException.class)
            .hasMessageContaining("reviewer")
            .hasMessageContaining("at least 2");
    }

    @Test void activateGroupRejectsNonFormingState() {
        registerSpec("spec-1", List.of());
        var gi = service.proposeGroup("spec-1", "t1", "agent-1");

        assertThatThrownBy(() -> service.activateGroup(gi.instanceId(), "t1"))
            .isInstanceOf(OrgValidationException.class)
            .hasMessageContaining("FORMING");
    }

    @Test void activateGroupWithNoRoleSpecs() {
        registerSpec("spec-1", List.of());
        var gi = service.createGroup("spec-1", "t1", FormationOrigin.TOP_DOWN);

        service.activateGroup(gi.instanceId(), "t1");

        var updated = service.findInstance(gi.instanceId(), "t1").orElseThrow();
        assertThat(updated.state()).isEqualTo(GroupLifecycleState.ACTIVE);
    }

    @Test void commitToMissionValidatesMissionExists() {
        registerSpecWithMissions("spec-m", List.of(), List.of(new MissionSpec("build-api", "Build the API")));
        var gi = service.createGroup("spec-m", "t1", FormationOrigin.TOP_DOWN);
        service.joinGroup(gi.instanceId(), "agent-1", "t1");
        assertThatThrownBy(() -> service.commitToMission(gi.instanceId(), "agent-1", "nonexistent", "t1"))
            .isInstanceOf(OrgValidationException.class)
            .hasMessageContaining("not defined");
    }

    @Test void commitToMissionValidatesRequiredRoles() {
        var mission = new MissionSpec("review", "Review work", Set.of("reviewer"), 0, 0);
        registerSpecWithMissions("spec-m", List.of(new RoleSpec("reviewer", null, 0, -1, Set.of(), List.of())), List.of(mission));
        var gi = service.createGroup("spec-m", "t1", FormationOrigin.TOP_DOWN);
        service.joinGroup(gi.instanceId(), "agent-1", "t1");
        assertThatThrownBy(() -> service.commitToMission(gi.instanceId(), "agent-1", "review", "t1"))
            .isInstanceOf(OrgValidationException.class)
            .hasMessageContaining("required role");

        service.adoptRole(gi.instanceId(), "agent-1", "reviewer", "t1");
        service.commitToMission(gi.instanceId(), "agent-1", "review", "t1");
        var updated = service.findInstance(gi.instanceId(), "t1").orElseThrow();
        assertThat(updated.missionCommitments()).hasSize(1);
    }

    @Test void commitToMissionEnforcesMaxCommitters() {
        var mission = new MissionSpec("solo-task", "Solo", Set.of(), 0, 1);
        registerSpecWithMissions("spec-m", List.of(), List.of(mission));
        var gi = service.createGroup("spec-m", "t1", FormationOrigin.TOP_DOWN);
        service.joinGroup(gi.instanceId(), "agent-1", "t1");
        service.joinGroup(gi.instanceId(), "agent-2", "t1");
        service.commitToMission(gi.instanceId(), "agent-1", "solo-task", "t1");
        assertThatThrownBy(() -> service.commitToMission(gi.instanceId(), "agent-2", "solo-task", "t1"))
            .isInstanceOf(OrgValidationException.class)
            .hasMessageContaining("max committers");
    }

    @Test void withdrawFromMissionValidatesCommitmentExists() {
        registerSpecWithMissions("spec-m", List.of(), List.of(new MissionSpec("build-api", "Build")));
        var gi = service.createGroup("spec-m", "t1", FormationOrigin.TOP_DOWN);
        service.joinGroup(gi.instanceId(), "agent-1", "t1");
        assertThatThrownBy(() -> service.withdrawFromMission(gi.instanceId(), "agent-1", "build-api", "t1"))
            .isInstanceOf(OrgValidationException.class)
            .hasMessageContaining("no commitment");
    }

    @Test void fulfillMissionValidatesMinCommitters() {
        var mission = new MissionSpec("team-task", "Team", Set.of(), 2, 0);
        registerSpecWithMissions("spec-m", List.of(), List.of(mission));
        var gi = service.createGroup("spec-m", "t1", FormationOrigin.TOP_DOWN);
        service.joinGroup(gi.instanceId(), "agent-1", "t1");
        service.commitToMission(gi.instanceId(), "agent-1", "team-task", "t1");
        assertThatThrownBy(() -> service.fulfillMission(gi.instanceId(), "team-task", "t1"))
            .isInstanceOf(OrgValidationException.class)
            .hasMessageContaining("at least 2");
    }

    @Test void fulfillMissionSucceedsWhenMinCommittersMet() {
        var mission = new MissionSpec("team-task", "Team", Set.of(), 2, 0);
        registerSpecWithMissions("spec-m", List.of(), List.of(mission));
        var gi = service.createGroup("spec-m", "t1", FormationOrigin.TOP_DOWN);
        service.joinGroup(gi.instanceId(), "agent-1", "t1");
        service.joinGroup(gi.instanceId(), "agent-2", "t1");
        service.commitToMission(gi.instanceId(), "agent-1", "team-task", "t1");
        service.commitToMission(gi.instanceId(), "agent-2", "team-task", "t1");
        service.fulfillMission(gi.instanceId(), "team-task", "t1");
        var updated = service.findInstance(gi.instanceId(), "t1").orElseThrow();
        assertThat(updated.missionCommitments()).isEmpty();
    }

    @Test void normSpecificityPreservedOnSpec() {
        var rules = new FormationRules(Set.of(FormationPath.TOP_DOWN), 2, 10, false, 1.0);
        registerSpecWithPolicy("spec-ns", List.of(), new GroupPolicy(rules, 3, NormSpecificity.DOMAIN));
        var spec = orgRegistry.findUnit("spec-ns", "t1").orElseThrow();
        assertThat(spec.policy().normSpecificity()).isEqualTo(NormSpecificity.DOMAIN);
        assertThat(spec.policy().normativePriority()).isEqualTo(3);
    }

    @Test void instancesForAgent() {
        registerSpec("spec-1", List.of());
        var gi = service.createGroup("spec-1", "t1", FormationOrigin.TOP_DOWN);
        service.joinGroup(gi.instanceId(), "agent-1", "t1");
        var results = service.instancesForAgent("agent-1", "t1");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).instanceId()).isEqualTo(gi.instanceId());
    }
}

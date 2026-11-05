package io.casehub.eidos.org.api;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrganizationalUnitTest {

    @Test void minimalUnit() {
        var unit = OrganizationalUnit.builder()
            .unitId("rig-1").name("Rig One").tenancyId("gastown").build();
        assertThat(unit.unitId()).isEqualTo("rig-1");
        assertThat(unit.members()).isEmpty();
        assertThat(unit.capabilities()).isEmpty();
        assertThat(unit.goals()).isEmpty();
        assertThat(unit.constraints()).isEmpty();
    }

    @Test void requiresUnitId() {
        assertThatThrownBy(() -> OrganizationalUnit.builder()
            .name("Rig").tenancyId("t").build())
            .isInstanceOf(NullPointerException.class);
    }

    @Test void requiresName() {
        assertThatThrownBy(() -> OrganizationalUnit.builder()
            .unitId("rig-1").tenancyId("t").build())
            .isInstanceOf(NullPointerException.class);
    }

    @Test void requiresTenancyId() {
        assertThatThrownBy(() -> OrganizationalUnit.builder()
            .unitId("rig-1").name("Rig").build())
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsDuplicateRolePairs() {
        var m1 = new Membership("agent-1", "witness", null);
        var m2 = new Membership("agent-1", "witness", null);
        assertThatThrownBy(() -> OrganizationalUnit.builder()
                                                   .unitId("rig-1").name("Rig").tenancyId("t")
                                                   .members(List.of(m1, m2)).build())
                .isInstanceOf(OrgValidationException.class)
                .hasMessageContaining("duplicate");
    }

    @Test void hasMember() {
        var unit = OrganizationalUnit.builder()
            .unitId("rig-1").name("Rig").tenancyId("t")
            .members(List.of(new Membership("agent-1", "witness", null))).build();
        assertThat(unit.hasMember("agent-1")).isTrue();
        assertThat(unit.hasMember("agent-2")).isFalse();
    }

    @Test void hierarchyViaParentUnitId() {
        var unit = OrganizationalUnit.builder()
            .unitId("rig-1").name("Rig").tenancyId("t")
            .parentUnitId("cluster-1").build();
        assertThat(unit.parentUnitId()).isEqualTo("cluster-1");
    }

    @Test void kindAndVocabulary() {
        var unit = OrganizationalUnit.builder()
            .unitId("rig-1").name("Rig").tenancyId("t")
            .kind("rig").kindVocabulary("urn:gastown:vocab:org").build();
        assertThat(unit.kind()).isEqualTo("rig");
        assertThat(unit.kindVocabulary()).isEqualTo("urn:gastown:vocab:org");
    }

    @Test void listsAreImmutable() {
        var unit = OrganizationalUnit.builder()
            .unitId("rig-1").name("Rig").tenancyId("t").build();
        assertThatThrownBy(() -> unit.members().add(new Membership("a", null, null)))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void withTopologyAndRoleSpecs() {
        var roleSpec = new RoleSpec("reviewer", null, 1, 3, Set.of(), List.of(), null, null);
        var unit = OrganizationalUnit.builder()
                                     .unitId("team-1").name("Review Team").tenancyId("t1")
                                     .topology("mesh").topologyVocabulary("urn:casehub:topologies")
                                     .authorityModel(AuthorityModel.DISTRIBUTED)
                                     .selfDetermination(0.5)
                                     .roleSpecs(List.of(roleSpec))
                                     .build();
        assertThat(unit.topology()).isEqualTo("mesh");
        assertThat(unit.authorityModel()).isEqualTo(AuthorityModel.DISTRIBUTED);
        assertThat(unit.selfDetermination()).isEqualTo(0.5);
        assertThat(unit.roleSpecs()).hasSize(1);
    }

    @Test
    void rejectsSelfDeterminationOutOfRange() {
        assertThatThrownBy(() -> OrganizationalUnit.builder()
                                                   .unitId("t").name("T").tenancyId("t1")
                                                   .selfDetermination(1.5).build())
                .isInstanceOf(OrgValidationException.class)
                .hasMessageContaining("selfDetermination");
    }

    @Test
    void rejectsSelfDeterminationNegative() {
        assertThatThrownBy(() -> OrganizationalUnit.builder()
                                                   .unitId("t").name("T").tenancyId("t1")
                                                   .selfDetermination(-0.1).build())
                .isInstanceOf(OrgValidationException.class)
                .hasMessageContaining("selfDetermination");
    }

    @Test
    void withGroupPolicy() {
        var rules  = new FormationRules(Set.of(FormationPath.TOP_DOWN), 2, 10, false, 1.0);
        var policy = new GroupPolicy(rules, 0, null, 0);
        var unit = OrganizationalUnit.builder()
                                     .unitId("t").name("T").tenancyId("t1")
                                     .policy(policy).build();
        assertThat(unit.policy().formationRules().allowedPaths()).containsExactly(FormationPath.TOP_DOWN);
    }

    @Test
    void rejectsDuplicateRoleSpecNames() {
        var r1 = new RoleSpec("reviewer", null, 0, -1, Set.of(), List.of(), null, null);
        var r2 = new RoleSpec("reviewer", null, 1, 5, Set.of(), List.of(), null, null);
        assertThatThrownBy(() -> OrganizationalUnit.builder()
                                                   .unitId("t").name("T").tenancyId("t1")
                                                   .roleSpecs(List.of(r1, r2)).build())
                .isInstanceOf(OrgValidationException.class)
                .hasMessageContaining("duplicate");
    }

    @Test
    void rejectsIncompatibleWithUnknownRole() {
        var r1 = new RoleSpec("reviewer", null, 0, -1, Set.of("unknown-role"), List.of(), null, null);
        assertThatThrownBy(() -> OrganizationalUnit.builder()
                                                   .unitId("t").name("T").tenancyId("t1")
                                                   .roleSpecs(List.of(r1)).build())
                .isInstanceOf(OrgValidationException.class)
                .hasMessageContaining("incompatibleWith");
    }

    @Test
    void allowsMultiRoleSeedMembership() {
        var r1 = new RoleSpec("reviewer", null, 0, -1, Set.of(), List.of(), null, null);
        var r2 = new RoleSpec("author", null, 0, -1, Set.of(), List.of(), null, null);
        var m1 = new Membership("agent-1", "reviewer", null);
        var m2 = new Membership("agent-1", "author", null);
        var unit = OrganizationalUnit.builder()
                                     .unitId("t").name("T").tenancyId("t1")
                                     .roleSpecs(List.of(r1, r2))
                                     .members(List.of(m1, m2)).build();
        assertThat(unit.members()).hasSize(2);
    }

    @Test
    void rejectsIncompatibleSeedMembership() {
        var r1 = new RoleSpec("auditor", null, 0, -1, Set.of("developer"), List.of(), null, null);
        var r2 = new RoleSpec("developer", null, 0, -1, Set.of("auditor"), List.of(), null, null);
        var m1 = new Membership("agent-1", "auditor", null);
        var m2 = new Membership("agent-1", "developer", null);
        assertThatThrownBy(() -> OrganizationalUnit.builder()
                                                   .unitId("t").name("T").tenancyId("t1")
                                                   .roleSpecs(List.of(r1, r2))
                                                   .members(List.of(m1, m2)).build())
                .isInstanceOf(OrgValidationException.class)
                .hasMessageContaining("incompatible");
    }
}

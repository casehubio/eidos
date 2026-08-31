package io.casehub.eidos.org.api;

import org.junit.jupiter.api.Test;

import java.util.List;

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

    @Test void rejectsDuplicateMembers() {
        var m1 = new Membership("agent-1", "witness", null);
        var m2 = new Membership("agent-1", "worker", null);
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
}

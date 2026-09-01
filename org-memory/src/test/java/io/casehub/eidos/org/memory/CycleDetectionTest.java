package io.casehub.eidos.org.memory;

import io.casehub.eidos.org.api.OrgValidationException;
import io.casehub.eidos.org.api.OrganizationalUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CycleDetectionTest {

    InMemoryOrgRegistry registry;

    @BeforeEach void setUp() { registry = new InMemoryOrgRegistry(); }

    @Test void detectsDirectCycle() {
        registry.registerUnit(OrganizationalUnit.builder()
            .unitId("a").name("A").tenancyId("t").build());
        assertThatThrownBy(() -> registry.registerUnit(OrganizationalUnit.builder()
            .unitId("a").name("A").tenancyId("t").parentUnitId("a").build()))
            .isInstanceOf(OrgValidationException.class)
            .hasMessageContaining("cycle");
    }

    @Test void detectsIndirectCycle() {
        registry.registerUnit(OrganizationalUnit.builder()
            .unitId("a").name("A").tenancyId("t").build());
        registry.registerUnit(OrganizationalUnit.builder()
            .unitId("b").name("B").tenancyId("t").parentUnitId("a").build());
        assertThatThrownBy(() -> registry.registerUnit(OrganizationalUnit.builder()
            .unitId("a").name("A").tenancyId("t").parentUnitId("b").build()))
            .isInstanceOf(OrgValidationException.class)
            .hasMessageContaining("cycle");
    }

    @Test void detectsThreeNodeCycle() {
        registry.registerUnit(OrganizationalUnit.builder()
            .unitId("a").name("A").tenancyId("t").build());
        registry.registerUnit(OrganizationalUnit.builder()
            .unitId("b").name("B").tenancyId("t").parentUnitId("a").build());
        registry.registerUnit(OrganizationalUnit.builder()
            .unitId("c").name("C").tenancyId("t").parentUnitId("b").build());
        assertThatThrownBy(() -> registry.registerUnit(OrganizationalUnit.builder()
            .unitId("a").name("A").tenancyId("t").parentUnitId("c").build()))
            .isInstanceOf(OrgValidationException.class)
            .hasMessageContaining("cycle");
    }

    @Test void allowsDeepHierarchy() {
        registry.registerUnit(OrganizationalUnit.builder()
            .unitId("l1").name("L1").tenancyId("t").build());
        registry.registerUnit(OrganizationalUnit.builder()
            .unitId("l2").name("L2").tenancyId("t").parentUnitId("l1").build());
        registry.registerUnit(OrganizationalUnit.builder()
            .unitId("l3").name("L3").tenancyId("t").parentUnitId("l2").build());
        registry.registerUnit(OrganizationalUnit.builder()
            .unitId("l4").name("L4").tenancyId("t").parentUnitId("l3").build());
        assertThat(registry.ancestorUnits("l4", "t")).hasSize(3);
    }

    @Test void parentInDifferentTenancyAllowed() {
        registry.registerUnit(OrganizationalUnit.builder()
            .unitId("a").name("A").tenancyId("t1").build());
        registry.registerUnit(OrganizationalUnit.builder()
            .unitId("b").name("B").tenancyId("t2").parentUnitId("a").build());
        assertThat(registry.findUnit("b", "t2")).isPresent();
    }

    @Test void differentTenancySameIdNoCrossContamination() {
        registry.registerUnit(OrganizationalUnit.builder()
            .unitId("root").name("Root").tenancyId("t1").build());
        registry.registerUnit(OrganizationalUnit.builder()
            .unitId("child").name("Child").tenancyId("t1").parentUnitId("root").build());
        registry.registerUnit(OrganizationalUnit.builder()
            .unitId("root").name("Root").tenancyId("t2").build());
        registry.registerUnit(OrganizationalUnit.builder()
            .unitId("child").name("Child").tenancyId("t2").parentUnitId("root").build());
        assertThat(registry.ancestorUnits("child", "t1")).hasSize(1);
        assertThat(registry.ancestorUnits("child", "t2")).hasSize(1);
    }
}

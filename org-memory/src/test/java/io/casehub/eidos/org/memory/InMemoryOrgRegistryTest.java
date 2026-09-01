package io.casehub.eidos.org.memory;

import io.casehub.eidos.org.api.AgentRelationship;
import io.casehub.eidos.org.api.Membership;
import io.casehub.eidos.org.api.OrgQuery;
import io.casehub.eidos.org.api.OrganizationalUnit;
import io.casehub.eidos.org.api.RelationshipKind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryOrgRegistryTest {

    InMemoryOrgRegistry registry;

    @BeforeEach void setUp() { registry = new InMemoryOrgRegistry(); }

    @Test void registerAndFindUnit() {
        var unit = OrganizationalUnit.builder()
            .unitId("rig-1").name("Rig One").tenancyId("t").build();
        registry.registerUnit(unit);
        assertThat(registry.findUnit("rig-1", "t")).isPresent();
        assertThat(registry.findUnit("rig-1", "other")).isEmpty();
    }

    @Test void removeUnit() {
        var unit = OrganizationalUnit.builder()
            .unitId("rig-1").name("Rig").tenancyId("t").build();
        registry.registerUnit(unit);
        registry.removeUnit("rig-1", "t");
        assertThat(registry.findUnit("rig-1", "t")).isEmpty();
    }

    @Test void findUnitsByKind() {
        registry.registerUnit(OrganizationalUnit.builder()
            .unitId("rig-1").name("Rig 1").kind("rig").tenancyId("t").build());
        registry.registerUnit(OrganizationalUnit.builder()
            .unitId("squad-1").name("Squad 1").kind("squad").tenancyId("t").build());
        var rigs = registry.findUnits(OrgQuery.byKind("rig", "t"));
        assertThat(rigs).hasSize(1);
        assertThat(rigs.getFirst().unitId()).isEqualTo("rig-1");
    }

    @Test void findAllUnits() {
        registry.registerUnit(OrganizationalUnit.builder()
            .unitId("a").name("A").tenancyId("t").build());
        registry.registerUnit(OrganizationalUnit.builder()
            .unitId("b").name("B").tenancyId("t").build());
        registry.registerUnit(OrganizationalUnit.builder()
            .unitId("c").name("C").tenancyId("other").build());
        assertThat(registry.findUnits(OrgQuery.all("t"))).hasSize(2);
    }

    @Test void childAndAncestorUnits() {
        registry.registerUnit(OrganizationalUnit.builder()
            .unitId("cluster").name("Cluster").tenancyId("t").build());
        registry.registerUnit(OrganizationalUnit.builder()
            .unitId("rig-1").name("Rig").tenancyId("t").parentUnitId("cluster").build());
        assertThat(registry.childUnits("cluster", "t")).hasSize(1);
        assertThat(registry.ancestorUnits("rig-1", "t")).hasSize(1);
        assertThat(registry.ancestorUnits("rig-1", "t").getFirst().unitId()).isEqualTo("cluster");
    }

    @Test void deepAncestorChain() {
        registry.registerUnit(OrganizationalUnit.builder()
            .unitId("l1").name("L1").tenancyId("t").build());
        registry.registerUnit(OrganizationalUnit.builder()
            .unitId("l2").name("L2").tenancyId("t").parentUnitId("l1").build());
        registry.registerUnit(OrganizationalUnit.builder()
            .unitId("l3").name("L3").tenancyId("t").parentUnitId("l2").build());
        var ancestors = registry.ancestorUnits("l3", "t");
        assertThat(ancestors).hasSize(2);
        assertThat(ancestors).extracting(OrganizationalUnit::unitId)
            .containsExactly("l2", "l1");
    }

    @Test void unitsForAgent() {
        registry.registerUnit(OrganizationalUnit.builder()
            .unitId("rig-1").name("Rig").tenancyId("t")
            .members(List.of(new Membership("agent-1", "witness", null))).build());
        registry.registerUnit(OrganizationalUnit.builder()
            .unitId("rig-2").name("Rig 2").tenancyId("t").build());
        assertThat(registry.unitsFor("agent-1", "t")).hasSize(1);
        assertThat(registry.unitsFor("agent-2", "t")).isEmpty();
    }

    @Test void membersOf() {
        registry.registerUnit(OrganizationalUnit.builder()
            .unitId("rig-1").name("Rig").tenancyId("t")
            .members(List.of(
                new Membership("a1", "witness", null),
                new Membership("a2", "worker", null))).build());
        assertThat(registry.membersOf("rig-1", "t")).hasSize(2);
        assertThat(registry.membersOf("nonexistent", "t")).isEmpty();
    }

    @Test void addAndQueryRelationships() {
        var rel = AgentRelationship.builder()
            .sourceAgentId("witness").targetAgentId("polecat")
            .kind(RelationshipKind.SUPERVISES).tenancyId("t").build();
        registry.addRelationship(rel);
        assertThat(registry.relationshipsFrom("witness", "t")).hasSize(1);
        assertThat(registry.relationshipsTo("polecat", "t")).hasSize(1);
        assertThat(registry.supervisors("polecat", "t")).hasSize(1);
        assertThat(registry.subordinates("witness", "t")).hasSize(1);
    }

    @Test void removeRelationship() {
        registry.addRelationship(AgentRelationship.builder()
            .sourceAgentId("a").targetAgentId("b")
            .kind(RelationshipKind.SUPERVISES).tenancyId("t").build());
        registry.removeRelationship("a", "b", RelationshipKind.SUPERVISES, "t");
        assertThat(registry.relationshipsFrom("a", "t")).isEmpty();
    }

    @Test void tenancyIsolation() {
        registry.addRelationship(AgentRelationship.builder()
            .sourceAgentId("a").targetAgentId("b")
            .kind(RelationshipKind.SUPERVISES).tenancyId("t1").build());
        assertThat(registry.relationshipsFrom("a", "t1")).hasSize(1);
        assertThat(registry.relationshipsFrom("a", "t2")).isEmpty();
    }

    @Test void escalationPath() {
        registry.addRelationship(AgentRelationship.builder()
            .sourceAgentId("polecat").targetAgentId("witness")
            .kind(RelationshipKind.ESCALATES_TO).tenancyId("t").build());
        registry.addRelationship(AgentRelationship.builder()
            .sourceAgentId("witness").targetAgentId("deacon")
            .kind(RelationshipKind.ESCALATES_TO).tenancyId("t").build());
        registry.addRelationship(AgentRelationship.builder()
            .sourceAgentId("deacon").targetAgentId("boot")
            .kind(RelationshipKind.ESCALATES_TO).tenancyId("t").build());
        var path = registry.escalationPath("polecat", "t");
        assertThat(path).hasSize(3);
        assertThat(path).extracting(AgentRelationship::targetAgentId)
            .containsExactly("witness", "deacon", "boot");
    }

    @Test void escalationPathStopsAtTerminal() {
        registry.addRelationship(AgentRelationship.builder()
            .sourceAgentId("a").targetAgentId("b")
            .kind(RelationshipKind.ESCALATES_TO).tenancyId("t").build());
        var path = registry.escalationPath("a", "t");
        assertThat(path).hasSize(1);
        assertThat(path.getFirst().targetAgentId()).isEqualTo("b");
    }
}

package io.casehub.eidos.org.memory;

import io.casehub.eidos.api.BehavioralSignal;
import io.casehub.eidos.org.api.AgentRelationship;
import io.casehub.eidos.org.api.AttestationGrant;
import io.casehub.eidos.org.api.Membership;
import io.casehub.eidos.org.api.OrganizationalUnit;
import io.casehub.eidos.org.api.RelationshipKind;
import io.casehub.eidos.org.api.RelationshipScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GastownScenarioTest {

    InMemoryOrgRegistry registry;

    @BeforeEach void setUp() {
        registry = new InMemoryOrgRegistry();

        // Oversight chain
        registry.registerUnit(OrganizationalUnit.builder()
            .unitId("oversight").name("Oversight Chain")
            .kind("supervision-hierarchy").tenancyId("gastown")
            .members(List.of(
                new Membership("boot", "root-watchdog", null),
                new Membership("deacon", "cross-rig-watchdog", null)))
            .build());

        // Rig Alpha
        registry.registerUnit(OrganizationalUnit.builder()
            .unitId("rig-alpha").name("Rig Alpha")
            .kind("rig").tenancyId("gastown")
            .members(List.of(
                new Membership("witness-alpha", "witness", null),
                new Membership("polecat-1", "worker", null),
                new Membership("polecat-2", "worker", null)))
            .build());

        // Boot → Deacon
        registry.addRelationship(AgentRelationship.builder()
            .sourceAgentId("boot").targetAgentId("deacon")
            .kind(RelationshipKind.SUPERVISES).tenancyId("gastown").build());

        // Deacon → Witness (scoped, with attestation)
        registry.addRelationship(AgentRelationship.builder()
            .sourceAgentId("deacon").targetAgentId("witness-alpha")
            .kind(RelationshipKind.SUPERVISES).tenancyId("gastown")
            .scope(new RelationshipScope("rig-monitoring", null, null))
            .attestation(new AttestationGrant(
                Set.of("LATENCY", "ATTESTATION_RATE"), Set.of(),
                Set.of(BehavioralSignal.COMPLIANT, BehavioralSignal.VIOLATED)))
            .build());

        // Witness → Polecats
        registry.addRelationship(AgentRelationship.builder()
            .sourceAgentId("witness-alpha").targetAgentId("polecat-1")
            .kind(RelationshipKind.SUPERVISES).tenancyId("gastown").build());
        registry.addRelationship(AgentRelationship.builder()
            .sourceAgentId("witness-alpha").targetAgentId("polecat-2")
            .kind(RelationshipKind.SUPERVISES).tenancyId("gastown").build());

        // Polecat-1 backs up Polecat-2 for code-analysis
        registry.addRelationship(AgentRelationship.builder()
            .sourceAgentId("polecat-1").targetAgentId("polecat-2")
            .kind(RelationshipKind.BACKS_UP).tenancyId("gastown")
            .scope(new RelationshipScope("code-analysis", null, null))
            .build());

        // Escalation chain
        registry.addRelationship(AgentRelationship.builder()
            .sourceAgentId("polecat-1").targetAgentId("witness-alpha")
            .kind(RelationshipKind.ESCALATES_TO).tenancyId("gastown").build());
        registry.addRelationship(AgentRelationship.builder()
            .sourceAgentId("witness-alpha").targetAgentId("deacon")
            .kind(RelationshipKind.ESCALATES_TO).tenancyId("gastown").build());
        registry.addRelationship(AgentRelationship.builder()
            .sourceAgentId("deacon").targetAgentId("boot")
            .kind(RelationshipKind.ESCALATES_TO).tenancyId("gastown").build());
    }

    @Test void supervisorChain() {
        assertThat(registry.supervisors("polecat-1", "gastown"))
            .hasSize(1)
            .first().extracting(AgentRelationship::sourceAgentId).isEqualTo("witness-alpha");
        assertThat(registry.supervisors("witness-alpha", "gastown"))
            .hasSize(1)
            .first().extracting(AgentRelationship::sourceAgentId).isEqualTo("deacon");
        assertThat(registry.supervisors("deacon", "gastown"))
            .hasSize(1)
            .first().extracting(AgentRelationship::sourceAgentId).isEqualTo("boot");
        assertThat(registry.supervisors("boot", "gastown")).isEmpty();
    }

    @Test void subordinatesOfWitness() {
        var subs = registry.subordinates("witness-alpha", "gastown");
        assertThat(subs).hasSize(2);
        assertThat(subs).extracting(AgentRelationship::targetAgentId)
            .containsExactlyInAnyOrder("polecat-1", "polecat-2");
    }

    @Test void backupRelationship() {
        var backups = registry.relationshipsFrom("polecat-1", "gastown").stream()
            .filter(r -> r.kind() == RelationshipKind.BACKS_UP).toList();
        assertThat(backups).hasSize(1);
        assertThat(backups.getFirst().scope().capabilityName()).isEqualTo("code-analysis");
        assertThat(backups.getFirst().targetAgentId()).isEqualTo("polecat-2");
    }

    @Test void attestationGrant() {
        var deaconSupervision = registry.supervisors("witness-alpha", "gastown").getFirst();
        assertThat(deaconSupervision.attestation()).isNotNull();
        assertThat(deaconSupervision.attestation().dimensions())
            .containsExactlyInAnyOrder("LATENCY", "ATTESTATION_RATE");
        assertThat(deaconSupervision.attestation().signalTypes())
            .containsExactlyInAnyOrder(BehavioralSignal.COMPLIANT, BehavioralSignal.VIOLATED);
    }

    @Test void scopedSupervision() {
        var deaconSupervision = registry.supervisors("witness-alpha", "gastown").getFirst();
        assertThat(deaconSupervision.scope()).isNotNull();
        assertThat(deaconSupervision.scope().capabilityName()).isEqualTo("rig-monitoring");
    }

    @Test void membershipQueries() {
        assertThat(registry.unitsFor("polecat-1", "gastown")).hasSize(1);
        assertThat(registry.unitsFor("polecat-1", "gastown").getFirst().unitId())
            .isEqualTo("rig-alpha");
        assertThat(registry.membersOf("rig-alpha", "gastown")).hasSize(3);
        assertThat(registry.membersOf("rig-alpha", "gastown"))
            .extracting(Membership::role)
            .containsExactlyInAnyOrder("witness", "worker", "worker");
    }

    @Test void agentInMultipleUnits() {
        assertThat(registry.unitsFor("deacon", "gastown")).hasSize(1);
        assertThat(registry.unitsFor("deacon", "gastown").getFirst().unitId())
            .isEqualTo("oversight");
    }

    @Test void fullEscalationPath() {
        var path = registry.escalationPath("polecat-1", "gastown");
        assertThat(path).hasSize(3);
        assertThat(path).extracting(AgentRelationship::targetAgentId)
            .containsExactly("witness-alpha", "deacon", "boot");
    }

    @Test void escalationPathFromWitness() {
        var path = registry.escalationPath("witness-alpha", "gastown");
        assertThat(path).hasSize(2);
        assertThat(path).extracting(AgentRelationship::targetAgentId)
            .containsExactly("deacon", "boot");
    }

    @Test void bootHasNoEscalation() {
        assertThat(registry.escalationPath("boot", "gastown")).isEmpty();
    }
}

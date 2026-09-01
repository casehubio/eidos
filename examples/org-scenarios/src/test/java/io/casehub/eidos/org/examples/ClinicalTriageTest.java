package io.casehub.eidos.org.examples;

import io.casehub.eidos.api.BehavioralSignal;
import io.casehub.eidos.org.api.OrgStructure;
import io.casehub.eidos.org.api.RelationshipKind;
import io.casehub.eidos.org.memory.InMemoryOrgRegistry;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Clinical Triage — nested departments with delegation and constraints.
 *
 * Demonstrates: nested unit hierarchy (hospital → departments),
 * delegation across departments, supervisor attestation,
 * team goals, hard constraints, extended relationship kinds.
 *
 * A hospital has an ER and Radiology department. The attending
 * physician delegates imaging review to the radiologist. The triage
 * nurse reports to the attending. The charge nurse supervises
 * the triage nurse with attestation authority.
 */
class ClinicalTriageTest {

    @Test void hospitalOrganization() {
        var clinical = OrgStructure.define("metro-health")

            // Hospital — top-level unit
            .unit("hospital").name("Metro General").kind("organization")
                .capability("emergency-medicine")
                .capability("diagnostic-imaging")
                .add()

            // ER department — child of hospital
            .unit("er").name("Emergency Room").kind("department")
                .parentUnit("hospital")
                .member("charge-nurse", "charge")
                .member("triage-nurse", "triage")
                .member("attending", "physician")
                .member("er-scribe", "scribe")
                .capability("patient-triage")
                .capability("emergency-treatment")
                .add()

            // Radiology — child of hospital
            .unit("radiology").name("Radiology").kind("department")
                .parentUnit("hospital")
                .member("radiologist", "specialist")
                .member("rad-tech", "technician")
                .capability("diagnostic-imaging")
                .add()

            // Supervision within ER
            .supervises("attending", "triage-nurse").add()
            .supervises("charge-nurse", "triage-nurse")
                .attestation(
                    Set.of("LATENCY"),
                    Set.of(BehavioralSignal.COMPLIANT, BehavioralSignal.VIOLATED))
                .add()
            .supervises("attending", "er-scribe").add()
            .supervises("radiologist", "rad-tech").add()

            // Delegation across departments
            .delegatesTo("attending", "radiologist")
                .scope("imaging-review")
                .add()

            // Reporting: triage nurse reports to charge nurse
            .reportsTo("triage-nurse", "charge-nurse").add()

            // Escalation: triage → attending
            .escalatesTo("triage-nurse", "attending").add()

            // Extended: mentorship
            .extended("attending", "er-scribe", "mentors")
                .kindVocabulary("urn:clinical:vocab:org")
                .add()

            .build();

        var registry = new InMemoryOrgRegistry();
        clinical.registerAll(registry);

        // Nested hierarchy: ER and Radiology under Hospital
        assertThat(registry.childUnits("hospital", "metro-health")).hasSize(2);
        assertThat(registry.ancestorUnits("er", "metro-health"))
            .extracting(u -> u.unitId()).containsExactly("hospital");

        // Delegation across departments
        var delegation = registry.relationshipsFrom("attending", "metro-health").stream()
            .filter(r -> r.kind() == RelationshipKind.DELEGATES_TO).toList();
        assertThat(delegation).hasSize(1);
        assertThat(delegation.getFirst().targetAgentId()).isEqualTo("radiologist");
        assertThat(delegation.getFirst().scope().capabilityName()).isEqualTo("imaging-review");

        // Charge nurse has attestation authority over triage nurse
        var chargeSupervision = registry.supervisors("triage-nurse", "metro-health").stream()
            .filter(r -> r.sourceAgentId().equals("charge-nurse")).findFirst().orElseThrow();
        assertThat(chargeSupervision.attestation()).isNotNull();
        assertThat(chargeSupervision.attestation().dimensions()).contains("LATENCY");

        // Extended relationship
        var mentorship = registry.relationshipsFrom("attending", "metro-health").stream()
            .filter(r -> r.kind() == RelationshipKind.EXTENDED).toList();
        assertThat(mentorship).hasSize(1);
        assertThat(mentorship.getFirst().extendedKind()).isEqualTo("mentors");

        // Escalation from triage to attending
        var path = registry.escalationPath("triage-nurse", "metro-health");
        assertThat(path).hasSize(1);
        assertThat(path.getFirst().targetAgentId()).isEqualTo("attending");

        // Hospital collective capabilities
        var hospital = registry.findUnit("hospital", "metro-health").orElseThrow();
        assertThat(hospital.capabilities()).extracting(c -> c.name())
            .containsExactlyInAnyOrder("emergency-medicine", "diagnostic-imaging");
    }
}

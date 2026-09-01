package io.casehub.eidos.org.api;

import io.casehub.eidos.api.BehavioralSignal;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class OrgStructureDslTest {

    @Test void gastownFullTopology() {
        var org = OrgStructure.define("gastown")
            .unit("oversight").name("Oversight Chain").kind("supervision-hierarchy")
                .member("boot", "root-watchdog")
                .member("deacon", "cross-rig-watchdog")
                .add()
            .unit("rig-alpha").name("Rig Alpha").kind("rig")
                .member("witness-alpha", "witness")
                .member("polecat-1", "worker")
                .member("polecat-2", "worker")
                .capability("full-stack-code-work")
                .add()
            .supervises("boot", "deacon").add()
            .supervises("deacon", "witness-alpha")
                .scope("rig-monitoring")
                .attestation(
                    Set.of("LATENCY", "ATTESTATION_RATE"),
                    Set.of(BehavioralSignal.COMPLIANT, BehavioralSignal.VIOLATED))
                .add()
            .supervises("witness-alpha", "polecat-1").add()
            .supervises("witness-alpha", "polecat-2").add()
            .backsUp("polecat-1", "polecat-2").scope("code-analysis").add()
            .escalatesTo("polecat-1", "witness-alpha").add()
            .escalatesTo("witness-alpha", "deacon").add()
            .escalatesTo("deacon", "boot").add()
            .build();

        assertThat(org.units()).hasSize(2);
        assertThat(org.relationships()).hasSize(8);

        var oversight = org.units().getFirst();
        assertThat(oversight.unitId()).isEqualTo("oversight");
        assertThat(oversight.members()).hasSize(2);
        assertThat(oversight.kind()).isEqualTo("supervision-hierarchy");

        var rig = org.units().get(1);
        assertThat(rig.members()).hasSize(3);
        assertThat(rig.capabilities()).hasSize(1);
        assertThat(rig.capabilities().getFirst().name()).isEqualTo("full-stack-code-work");

        var scopedSupervision = org.relationships().stream()
            .filter(r -> r.sourceAgentId().equals("deacon") &&
                         r.targetAgentId().equals("witness-alpha"))
            .findFirst().orElseThrow();
        assertThat(scopedSupervision.scope().capabilityName()).isEqualTo("rig-monitoring");
        assertThat(scopedSupervision.attestation().dimensions())
            .containsExactlyInAnyOrder("LATENCY", "ATTESTATION_RATE");
    }

    @Test void flatTeamNoHierarchy() {
        var org = OrgStructure.define("devtown")
            .unit("review-team").name("Code Review Team").kind("team")
                .member("structural-reviewer", "structural")
                .member("content-reviewer", "content")
                .member("readability-reviewer", "readability")
                .capability("full-stack-review")
                .add()
            .build();

        assertThat(org.units()).hasSize(1);
        assertThat(org.relationships()).isEmpty();
        assertThat(org.units().getFirst().members()).hasSize(3);
    }

    @Test void tieredEscalation() {
        var org = OrgStructure.define("support")
            .unit("support-team").name("Support").kind("team")
                .member("l1-agent")
                .member("l2-agent")
                .member("l3-agent")
                .add()
            .escalatesTo("l1-agent", "l2-agent").scope("general").add()
            .escalatesTo("l2-agent", "l3-agent").scope("general").add()
            .supervises("l3-agent", "l2-agent").add()
            .supervises("l2-agent", "l1-agent").add()
            .build();

        assertThat(org.units()).hasSize(1);
        assertThat(org.relationships()).hasSize(4);
    }

    @Test void nestedHierarchy() {
        var org = OrgStructure.define("clinical")
            .unit("hospital").name("Main Hospital").kind("organization").add()
            .unit("er").name("Emergency Room").kind("department")
                .parentUnit("hospital")
                .member("triage-nurse", "triage")
                .member("attending", "physician")
                .add()
            .unit("radiology").name("Radiology").kind("department")
                .parentUnit("hospital")
                .member("radiologist", "specialist")
                .add()
            .delegatesTo("attending", "radiologist")
                .scope("imaging-review")
                .add()
            .build();

        assertThat(org.units()).hasSize(3);
        assertThat(org.units().get(1).parentUnitId()).isEqualTo("hospital");
        assertThat(org.relationships()).hasSize(1);
        assertThat(org.relationships().getFirst().kind()).isEqualTo(RelationshipKind.DELEGATES_TO);
    }

    @Test void extendedRelationshipKind() {
        var org = OrgStructure.define("custom")
            .unit("team").name("Team").kind("team")
                .member("mentor")
                .member("mentee")
                .add()
            .extended("mentor", "mentee", "mentors")
                .kindVocabulary("urn:custom:vocab:org")
                .add()
            .build();

        var rel = org.relationships().getFirst();
        assertThat(rel.kind()).isEqualTo(RelationshipKind.EXTENDED);
        assertThat(rel.extendedKind()).isEqualTo("mentors");
        assertThat(rel.kindVocabulary()).isEqualTo("urn:custom:vocab:org");
    }

    @Test void resultContainsBothUnitsAndRelationships() {
        var org = OrgStructure.define("t")
            .unit("u1").name("U1").kind("team")
                .member("a1", "lead")
                .add()
            .supervises("a1", "a2").add()
            .build();

        assertThat(org.units()).hasSize(1);
        assertThat(org.relationships()).hasSize(1);
        assertThat(org.units().getFirst().unitId()).isEqualTo("u1");
        assertThat(org.relationships().getFirst().kind()).isEqualTo(RelationshipKind.SUPERVISES);
    }
}

package io.casehub.eidos.org.runtime.yaml;

import io.casehub.eidos.org.api.RelationshipKind;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClasspathYamlOrgRegistrarTest {

    private final ClasspathYamlOrgRegistrar registrar = new ClasspathYamlOrgRegistrar();

    @Test void loadsGastownOrganization() {
        var yaml = getClass().getResourceAsStream("/gastown-org.yaml");
        var org = registrar.loadFrom(yaml);

        assertThat(org.units()).hasSize(2);
        assertThat(org.relationships()).hasSize(5);
    }

    @Test void unitsDeserializedCorrectly() {
        var yaml = getClass().getResourceAsStream("/gastown-org.yaml");
        var org = registrar.loadFrom(yaml);

        var oversight = org.units().stream()
            .filter(u -> u.unitId().equals("oversight")).findFirst().orElseThrow();
        assertThat(oversight.name()).isEqualTo("Oversight Chain");
        assertThat(oversight.kind()).isEqualTo("supervision-hierarchy");
        assertThat(oversight.tenancyId()).isEqualTo("gastown");
        assertThat(oversight.members()).hasSize(2);
        assertThat(oversight.members().getFirst().agentId()).isEqualTo("boot");
        assertThat(oversight.members().getFirst().role()).isEqualTo("root-watchdog");
    }

    @Test void rigHasCapabilities() {
        var yaml = getClass().getResourceAsStream("/gastown-org.yaml");
        var org = registrar.loadFrom(yaml);

        var rig = org.units().stream()
            .filter(u -> u.unitId().equals("rig-alpha")).findFirst().orElseThrow();
        assertThat(rig.capabilities()).hasSize(1);
        assertThat(rig.capabilities().getFirst().name()).isEqualTo("full-stack-code-work");
    }

    @Test void relationshipsDeserializedCorrectly() {
        var yaml = getClass().getResourceAsStream("/gastown-org.yaml");
        var org = registrar.loadFrom(yaml);

        var bootSupervises = org.relationships().stream()
            .filter(r -> r.sourceAgentId().equals("boot")).findFirst().orElseThrow();
        assertThat(bootSupervises.targetAgentId()).isEqualTo("deacon");
        assertThat(bootSupervises.kind()).isEqualTo(RelationshipKind.SUPERVISES);
        assertThat(bootSupervises.tenancyId()).isEqualTo("gastown");
    }

    @Test void scopedRelationshipDeserialized() {
        var yaml = getClass().getResourceAsStream("/gastown-org.yaml");
        var org = registrar.loadFrom(yaml);

        var scopedRel = org.relationships().stream()
            .filter(r -> r.sourceAgentId().equals("deacon") &&
                         r.targetAgentId().equals("witness-alpha"))
            .findFirst().orElseThrow();
        assertThat(scopedRel.scope()).isNotNull();
        assertThat(scopedRel.scope().capabilityName()).isEqualTo("rig-monitoring");
    }

    @Test void attestationGrantDeserialized() {
        var yaml = getClass().getResourceAsStream("/gastown-org.yaml");
        var org = registrar.loadFrom(yaml);

        var attested = org.relationships().stream()
            .filter(r -> r.attestation() != null).findFirst().orElseThrow();
        assertThat(attested.attestation().dimensions())
            .containsExactlyInAnyOrder("LATENCY", "ATTESTATION_RATE");
    }

    @Test void backupRelationshipWithScope() {
        var yaml = getClass().getResourceAsStream("/gastown-org.yaml");
        var org = registrar.loadFrom(yaml);

        var backup = org.relationships().stream()
            .filter(r -> r.kind() == RelationshipKind.BACKS_UP).findFirst().orElseThrow();
        assertThat(backup.sourceAgentId()).isEqualTo("polecat-1");
        assertThat(backup.targetAgentId()).isEqualTo("polecat-2");
        assertThat(backup.scope().capabilityName()).isEqualTo("code-analysis");
    }

    @Test void emptyYamlReturnsEmptyResult() {
        var org = registrar.loadFrom(null);
        assertThat(org.units()).isEmpty();
        assertThat(org.relationships()).isEmpty();
    }
}

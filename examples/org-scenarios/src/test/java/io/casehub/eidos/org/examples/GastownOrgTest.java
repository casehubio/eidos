package io.casehub.eidos.org.examples;

import io.casehub.eidos.api.BehavioralSignal;
import io.casehub.eidos.org.api.OrgStructure;
import io.casehub.eidos.org.memory.InMemoryOrgRegistry;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Gastown — 4-level supervision hierarchy with rigs.
 *
 * Demonstrates: deep hierarchy, scoped supervision, attestation grants,
 * escalation chains, backup agents, vocabulary-grounded unit kinds.
 *
 * Boot → Deacon → Witness → Polecat
 */
class GastownOrgTest {

    @Test void fullGastownOrganization() {
        var gastown = OrgStructure.define("gastown")

            // Oversight hierarchy
            .unit("oversight").name("Oversight Chain").kind("supervision-hierarchy")
                .member("boot", "root-watchdog")
                .member("deacon", "cross-rig-watchdog")
                .add()

            // Rig Alpha — a self-contained work unit
            .unit("rig-alpha").name("Rig Alpha").kind("rig")
                .member("witness-alpha", "witness")
                .member("polecat-1", "worker")
                .member("polecat-2", "worker")
                .capability("full-stack-code-work")
                .add()

            // Rig Beta — second rig, same structure
            .unit("rig-beta").name("Rig Beta").kind("rig")
                .member("witness-beta", "witness")
                .member("polecat-3", "worker")
                .capability("full-stack-code-work")
                .add()

            // Supervision: Boot → Deacon
            .supervises("boot", "deacon").add()

            // Supervision: Deacon → Witnesses (scoped to rig-monitoring, with attestation)
            .supervises("deacon", "witness-alpha")
                .scope("rig-monitoring")
                .attestation(
                    Set.of("LATENCY", "ATTESTATION_RATE"),
                    Set.of(BehavioralSignal.COMPLIANT, BehavioralSignal.VIOLATED))
                .add()
            .supervises("deacon", "witness-beta")
                .scope("rig-monitoring")
                .attestation(
                    Set.of("LATENCY", "ATTESTATION_RATE"),
                    Set.of(BehavioralSignal.COMPLIANT, BehavioralSignal.VIOLATED))
                .add()

            // Supervision: Witness → Polecats
            .supervises("witness-alpha", "polecat-1").add()
            .supervises("witness-alpha", "polecat-2").add()
            .supervises("witness-beta", "polecat-3").add()

            // Backup: within a rig
            .backsUp("polecat-1", "polecat-2").scope("code-analysis").add()

            // Escalation: Polecat → Witness → Deacon → Boot
            .escalatesTo("polecat-1", "witness-alpha").add()
            .escalatesTo("polecat-2", "witness-alpha").add()
            .escalatesTo("witness-alpha", "deacon").add()
            .escalatesTo("witness-beta", "deacon").add()
            .escalatesTo("deacon", "boot").add()

            .build();

        // Register and query
        var registry = new InMemoryOrgRegistry();
        gastown.registerAll(registry);

        // 4-level supervision chain
        assertThat(registry.supervisors("polecat-1", "gastown").getFirst().sourceAgentId())
            .isEqualTo("witness-alpha");
        assertThat(registry.supervisors("witness-alpha", "gastown").getFirst().sourceAgentId())
            .isEqualTo("deacon");
        assertThat(registry.supervisors("deacon", "gastown").getFirst().sourceAgentId())
            .isEqualTo("boot");

        // Full escalation path from polecat to boot
        var path = registry.escalationPath("polecat-1", "gastown");
        assertThat(path).extracting(r -> r.targetAgentId())
            .containsExactly("witness-alpha", "deacon", "boot");

        // Scoped attestation on Deacon → Witness
        var deaconSupervision = registry.supervisors("witness-alpha", "gastown").getFirst();
        assertThat(deaconSupervision.attestation().dimensions())
            .containsExactlyInAnyOrder("LATENCY", "ATTESTATION_RATE");

        // Two rigs, each with their witness
        assertThat(registry.findUnits(
            io.casehub.eidos.org.api.OrgQuery.byKind("rig", "gastown"))).hasSize(2);
    }
}

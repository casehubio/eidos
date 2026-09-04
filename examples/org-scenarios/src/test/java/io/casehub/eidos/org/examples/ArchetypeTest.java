package io.casehub.eidos.org.examples;

import io.casehub.eidos.org.api.OrgQuery;
import io.casehub.eidos.org.api.RelationshipKind;
import io.casehub.eidos.org.memory.InMemoryOrgRegistry;
import io.casehub.eidos.org.runtime.yaml.ClasspathYamlOrgRegistrar;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Organizational archetype examples — one YAML per archetype,
 * mapped to formal org theory (Mintzberg, Horling &amp; Lesser, MOISE+).
 *
 * See specs/organizational-archetypes-research.md for full taxonomy.
 */
class ArchetypeTest {

    private final ClasspathYamlOrgRegistrar registrar = new ClasspathYamlOrgRegistrar();

    private InMemoryOrgRegistry load(String name) {
        var yaml = getClass().getResourceAsStream("/archetypes/" + name + ".yaml");
        assertThat(yaml).as("YAML resource for archetype: %s", name).isNotNull();
        var org = registrar.loadFrom(yaml);
        var registry = new InMemoryOrgRegistry();
        org.units().forEach(registry::registerUnit);
        org.relationships().forEach(registry::addRelationship);
        return registry;
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "simple-structure",
        "professional-bureaucracy",
        "tiered-escalation",
        "divisional-holarchy",
        "federation-orchestrator",
        "pipeline",
        "coalition-advisory",
        "matrix",
        "market"
    })
    void archetypeLoadsAndRegisters(String archetype) {
        var registry = load(archetype);
        assertThat(registry).isNotNull();
    }

    // --- Simple Structure (Mintzberg) ---

    @Test
    void simpleStructure_flatSupervision() {
        var registry = load("simple-structure");
        var subs = registry.subordinates("ceo", "archetype-simple");
        assertThat(subs).hasSize(4);
        assertThat(subs).extracting(r -> r.targetAgentId())
            .containsExactlyInAnyOrder("dev-1", "dev-2", "designer", "ops");
    }

    @Test
    void simpleStructure_unitHasGoalAndConstraint() {
        var registry = load("simple-structure");
        var unit = registry.findUnit("startup", "archetype-simple").orElseThrow();
        assertThat(unit.goals()).hasSize(1);
        assertThat(unit.goals().getFirst().name()).isEqualTo("ship-mvp");
        assertThat(unit.constraints()).hasSize(1);
        assertThat(unit.constraints().getFirst().name()).isEqualTo("budget-cap");
    }

    // --- Professional Bureaucracy (Mintzberg) / Team (Horling & Lesser) ---

    @Test
    void professionalBureaucracy_noHierarchy() {
        var registry = load("professional-bureaucracy");
        var unit = registry.findUnit("review-team", "archetype-proburc").orElseThrow();
        assertThat(unit.members()).hasSize(4);
        assertThat(registry.subordinates("reviewer-backend", "archetype-proburc")).isEmpty();
        assertThat(registry.supervisors("reviewer-backend", "archetype-proburc")).isEmpty();
    }

    @Test
    void professionalBureaucracy_mutualBackup() {
        var registry = load("professional-bureaucracy");
        var backups = registry.relationshipsFrom("reviewer-backend", "archetype-proburc").stream()
            .filter(r -> r.kind() == RelationshipKind.BACKS_UP).toList();
        assertThat(backups).hasSize(1);
        assertThat(backups.getFirst().targetAgentId()).isEqualTo("reviewer-frontend");
    }

    // --- Tiered Escalation ---

    @Test
    void tieredEscalation_threeLevel() {
        var registry = load("tiered-escalation");
        var path = registry.escalationPath("l1-agent-1", "archetype-escalation");
        assertThat(path).extracting(r -> r.targetAgentId())
            .containsExactly("l2-billing", "l3-engineer");
    }

    @Test
    void tieredEscalation_domainScopedEscalation() {
        var registry = load("tiered-escalation");
        var escalations = registry.relationshipsFrom("l1-agent-1", "archetype-escalation").stream()
            .filter(r -> r.kind() == RelationshipKind.ESCALATES_TO).toList();
        assertThat(escalations).hasSize(2);
        assertThat(escalations).extracting(r -> r.scope().capabilityName())
            .containsExactlyInAnyOrder("billing-support", "technical-support");
    }

    // --- Divisional / Holarchy ---

    @Test
    void holarchy_nestedUnits() {
        var registry = load("divisional-holarchy");
        var children = registry.childUnits("hospital", "archetype-holarchy");
        assertThat(children).hasSize(2);
        assertThat(children).extracting(u -> u.unitId())
            .containsExactlyInAnyOrder("emergency", "radiology");
    }

    @Test
    void holarchy_crossDeptDelegation() {
        var registry = load("divisional-holarchy");
        var delegations = registry.relationshipsFrom("er-attending", "archetype-holarchy").stream()
            .filter(r -> r.kind() == RelationshipKind.DELEGATES_TO).toList();
        assertThat(delegations).hasSize(1);
        assertThat(delegations.getFirst().targetAgentId()).isEqualTo("radiologist");
        assertThat(delegations.getFirst().scope().capabilityName()).isEqualTo("imaging");
        assertThat(delegations.getFirst().scope().domain()).isEqualTo("emergency");
    }

    @Test
    void holarchy_extendedMentorship() {
        var registry = load("divisional-holarchy");
        var extended = registry.relationshipsFrom("er-attending", "archetype-holarchy").stream()
            .filter(r -> r.kind() == RelationshipKind.EXTENDED).toList();
        assertThat(extended).hasSize(1);
        assertThat(extended.getFirst().extendedKind()).isEqualTo("mentors");
    }

    // --- Federation / Orchestrator-Worker ---

    @Test
    void federation_coordinatorDelegatesToAll() {
        var registry = load("federation-orchestrator");
        var delegations = registry.relationshipsFrom("orchestrator", "archetype-federation").stream()
            .filter(r -> r.kind() == RelationshipKind.DELEGATES_TO).toList();
        assertThat(delegations).hasSize(4);
        assertThat(delegations).extracting(r -> r.targetAgentId())
            .containsExactlyInAnyOrder("searcher", "coder", "tester", "reviewer");
    }

    @Test
    void federation_specialistsReportBack() {
        var registry = load("federation-orchestrator");
        var reports = registry.relationshipsTo("orchestrator", "archetype-federation").stream()
            .filter(r -> r.kind() == RelationshipKind.REPORTS_TO).toList();
        assertThat(reports).hasSize(4);
    }

    @Test
    void federation_attestation() {
        var registry = load("federation-orchestrator");
        var attests = registry.relationshipsFrom("reviewer", "archetype-federation").stream()
            .filter(r -> r.kind() == RelationshipKind.EXTENDED).toList();
        assertThat(attests).hasSize(1);
        assertThat(attests.getFirst().extendedKind()).isEqualTo("attests-quality");
        assertThat(attests.getFirst().attestation()).isNotNull();
        assertThat(attests.getFirst().attestation().dimensions())
            .containsExactlyInAnyOrder("quality", "correctness");
    }

    // --- Pipeline ---

    @Test
    void pipeline_sequentialChain() {
        var registry = load("pipeline");
        var fromResearcher = registry.relationshipsFrom("researcher", "archetype-pipeline").stream()
            .filter(r -> r.kind() == RelationshipKind.DELEGATES_TO).toList();
        assertThat(fromResearcher).hasSize(1);
        assertThat(fromResearcher.getFirst().targetAgentId()).isEqualTo("writer");

        var fromWriter = registry.relationshipsFrom("writer", "archetype-pipeline").stream()
            .filter(r -> r.kind() == RelationshipKind.DELEGATES_TO).toList();
        assertThat(fromWriter).hasSize(1);
        assertThat(fromWriter.getFirst().targetAgentId()).isEqualTo("editor");

        var fromEditor = registry.relationshipsFrom("editor", "archetype-pipeline").stream()
            .filter(r -> r.kind() == RelationshipKind.DELEGATES_TO).toList();
        assertThat(fromEditor).hasSize(1);
        assertThat(fromEditor.getFirst().targetAgentId()).isEqualTo("publisher");
    }

    @Test
    void pipeline_rejectionPath() {
        var registry = load("pipeline");
        var rejection = registry.relationshipsFrom("editor", "archetype-pipeline").stream()
            .filter(r -> r.kind() == RelationshipKind.ESCALATES_TO).toList();
        assertThat(rejection).hasSize(1);
        assertThat(rejection.getFirst().targetAgentId()).isEqualTo("writer");
        assertThat(rejection.getFirst().scope().custom()).isEqualTo("quality-below-threshold");
    }

    // --- Coalition / Advisory Board ---

    @Test
    void coalition_advisorsReportToJudge() {
        var registry = load("coalition-advisory");
        var reports = registry.relationshipsTo("judge", "archetype-coalition").stream()
            .filter(r -> r.kind() == RelationshipKind.REPORTS_TO).toList();
        assertThat(reports).hasSize(3);
        assertThat(reports).extracting(r -> r.scope().domain())
            .containsExactlyInAnyOrder("security", "performance", "maintainability");
    }

    @Test
    void coalition_advisorAttestation() {
        var registry = load("coalition-advisory");
        var advises = registry.relationshipsFrom("advisor-security", "archetype-coalition").stream()
            .filter(r -> r.kind() == RelationshipKind.EXTENDED).toList();
        assertThat(advises).hasSize(1);
        assertThat(advises.getFirst().extendedKind()).isEqualTo("advises");
        assertThat(advises.getFirst().attestation().dimensions()).contains("security-posture");
    }

    // --- Matrix ---

    @Test
    void matrix_agentInMultipleUnits() {
        var registry = load("matrix");
        var aliceUnits = registry.unitsFor("dev-alice", "archetype-matrix");
        assertThat(aliceUnits).hasSize(2);
        assertThat(aliceUnits).extracting(u -> u.unitId())
            .containsExactlyInAnyOrder("platform-team", "billing-project");
    }

    @Test
    void matrix_dualReporting() {
        var registry = load("matrix");
        var aliceSupervisors = registry.supervisors("dev-alice", "archetype-matrix");
        assertThat(aliceSupervisors).hasSize(1);
        assertThat(aliceSupervisors.getFirst().sourceAgentId()).isEqualTo("platform-lead");

        var aliceReports = registry.relationshipsFrom("dev-alice", "archetype-matrix").stream()
            .filter(r -> r.kind() == RelationshipKind.REPORTS_TO).toList();
        assertThat(aliceReports).hasSize(1);
        assertThat(aliceReports.getFirst().targetAgentId()).isEqualTo("project-mgr");
    }

    // --- Market ---

    @Test
    void market_biddersToAuctioneer() {
        var registry = load("market");
        var bids = registry.relationshipsTo("auctioneer", "archetype-market").stream()
            .filter(r -> r.kind() == RelationshipKind.EXTENDED
                && "bids-to".equals(r.extendedKind())).toList();
        assertThat(bids).hasSize(3);
    }

    @Test
    void market_auctioneerDelegates() {
        var registry = load("market");
        var delegations = registry.relationshipsFrom("auctioneer", "archetype-market").stream()
            .filter(r -> r.kind() == RelationshipKind.DELEGATES_TO).toList();
        assertThat(delegations).hasSize(3);
        assertThat(delegations).allMatch(r -> "awarded-by-auction".equals(r.scope().custom()));
    }
}

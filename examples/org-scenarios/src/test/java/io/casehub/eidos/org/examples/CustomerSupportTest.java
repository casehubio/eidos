package io.casehub.eidos.org.examples;

import io.casehub.eidos.org.api.OrgStructure;
import io.casehub.eidos.org.api.RelationshipKind;
import io.casehub.eidos.org.memory.InMemoryOrgRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Customer Support — 3-tier escalation with domain-scoped supervision.
 *
 * Demonstrates: tiered escalation (L1 → L2 → L3), domain-scoped
 * supervision, backup agents, extended relationship kinds.
 *
 * L1 handles general inquiries. L2 handles technical issues.
 * L3 handles critical/security issues. Each tier supervises the one below.
 * Escalation paths are domain-scoped.
 */
class CustomerSupportTest {

    @Test void tieredSupport() {
        var support = OrgStructure.define("acme-support")

            .unit("support-org").name("Customer Support").kind("department")
                .member("l1-general", "tier-1")
                .member("l1-billing", "tier-1")
                .member("l2-technical", "tier-2")
                .member("l2-account", "tier-2")
                .member("l3-security", "tier-3")
                .member("l3-escalation", "tier-3")
                .capability("customer-issue-resolution")
                .add()

            // Supervision: L3 → L2 → L1
            .supervises("l3-escalation", "l2-technical").add()
            .supervises("l3-escalation", "l2-account").add()
            .supervises("l2-technical", "l1-general").add()
            .supervises("l2-account", "l1-billing").add()

            // Domain-scoped escalation
            .escalatesTo("l1-general", "l2-technical")
                .scope("technical")
                .add()
            .escalatesTo("l1-billing", "l2-account")
                .scope("billing")
                .add()
            .escalatesTo("l2-technical", "l3-security")
                .scope("security")
                .add()
            .escalatesTo("l2-account", "l3-escalation")
                .scope("billing", "critical")
                .add()

            // Backup: L1 agents cover each other
            .backsUp("l1-general", "l1-billing").add()
            .backsUp("l1-billing", "l1-general").add()

            // L2 backup
            .backsUp("l2-technical", "l2-account").scope("general").add()

            .build();

        var registry = new InMemoryOrgRegistry();
        support.registerAll(registry);

        // 3-tier supervision
        assertThat(registry.supervisors("l1-general", "acme-support"))
            .hasSize(1)
            .first().extracting(r -> r.sourceAgentId()).isEqualTo("l2-technical");

        // Domain-scoped escalation from L1 → L2
        var l1Escalation = registry.escalationPath("l1-general", "acme-support");
        assertThat(l1Escalation.getFirst().scope().capabilityName()).isEqualTo("technical");
        assertThat(l1Escalation.getFirst().targetAgentId()).isEqualTo("l2-technical");

        // Full escalation: L1 → L2 → L3
        assertThat(l1Escalation).hasSize(2);
        assertThat(l1Escalation.get(1).targetAgentId()).isEqualTo("l3-security");

        // Mutual backup at L1
        var l1Backups = registry.relationshipsFrom("l1-general", "acme-support").stream()
            .filter(r -> r.kind() == RelationshipKind.BACKS_UP).toList();
        assertThat(l1Backups).hasSize(1);
        assertThat(l1Backups.getFirst().targetAgentId()).isEqualTo("l1-billing");
    }
}

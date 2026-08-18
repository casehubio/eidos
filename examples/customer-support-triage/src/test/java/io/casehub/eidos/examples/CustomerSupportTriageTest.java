package io.casehub.eidos.examples;

import io.casehub.eidos.api.AgentRegistry;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class CustomerSupportTriageTest {

    @Inject
    AgentRegistry registry;

    @Test
    void autoDerivesIdFromClassName() {
        var d = registry.findById("customer-support-triage", "default").orElseThrow();
        assertThat(d.agentId()).isEqualTo("customer-support-triage");
    }

    @Test
    void autoDerivesDisplayName() {
        var d = registry.findById("customer-support-triage", "default").orElseThrow();
        assertThat(d.name()).isEqualTo("Customer Support Triage");
    }

    @Test
    void hasSlotAndBriefing() {
        var d = registry.findById("customer-support-triage", "default").orElseThrow();
        assertThat(d.slot()).isEqualTo("triage");
        assertThat(d.briefing()).isEqualTo("Routes incoming support tickets to the right team based on urgency and topic");
    }

    @Test
    void identityOnlyHasNullDisposition() {
        var d = registry.findById("customer-support-triage", "default").orElseThrow();
        assertThat(d.disposition()).isNull();
    }

    @Test
    void identityOnlyHasNoCapabilitiesGoalsOrConstraints() {
        var d = registry.findById("customer-support-triage", "default").orElseThrow();
        assertThat(d.capabilities()).isEmpty();
        assertThat(d.goals()).isEmpty();
        assertThat(d.constraints()).isEmpty();
    }
}

package io.casehub.eidos.examples;

import io.casehub.eidos.api.AgentRegistry;
import io.casehub.eidos.api.ConstraintSeverity;
import io.casehub.eidos.api.DispositionAxis;
import io.casehub.eidos.api.GoalPriority;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

@QuarkusTest
class LegalAnalystAgentTest {

    @Inject
    AgentRegistry registry;

    @Test
    void legalAnalystIsRegistered() {
        var d = registry.findById("legal-analyst-agent", "default").orElseThrow();
        assertThat(d.slot()).isEqualTo("legal-analyst");
        assertThat(d.jurisdiction()).isEqualTo("EU");
        assertThat(d.dataHandlingPolicy()).isEqualTo("gdpr-compliant");
    }

    @Test
    void hasDisposition() {
        var d = registry.findById("legal-analyst-agent", "default").orElseThrow();
        assertThat(d.disposition().primaryTerm(DispositionAxis.SOCIAL_ORIENTATION)).isEqualTo("collaborative");
        assertThat(d.disposition().primaryTerm(DispositionAxis.RULE_FOLLOWING)).isEqualTo("strict");
        assertThat(d.disposition().primaryTerm(DispositionAxis.CONFLICT_MODE)).isEqualTo("accommodating");
    }

    @Test
    void hasCapabilities() {
        var d = registry.findById("legal-analyst-agent", "default").orElseThrow();
        assertThat(d.capabilities()).hasSize(3);
        assertThat(d.capabilities().stream().map(c -> c.name()).toList())
            .containsExactly("document-analysis", "clause-extraction", "risk-assessment");
    }

    @Test
    void hasGoals() {
        var d = registry.findById("legal-analyst-agent", "default").orElseThrow();
        assertThat(d.goals()).hasSize(2);
        var primary = d.goals().stream().filter(g -> g.name().equals("accurate-analysis")).findFirst().orElseThrow();
        assertThat(primary.priority()).isEqualTo(GoalPriority.PRIMARY);
        assertThat(primary.capabilities()).containsExactly("document-analysis");
    }

    @Test
    void hasConstraints() {
        var d = registry.findById("legal-analyst-agent", "default").orElseThrow();
        assertThat(d.constraints()).hasSize(2);
        var hard = d.constraints().stream().filter(c -> c.name().equals("no-legal-advice")).findFirst().orElseThrow();
        assertThat(hard.severity()).isEqualTo(ConstraintSeverity.HARD);
    }
}

package io.casehub.eidos.examples;

import io.casehub.eidos.api.AgentRegistry;
import io.casehub.eidos.api.ConstraintSeverity;
import io.casehub.eidos.api.GoalPriority;
import io.casehub.eidos.api.Visibility;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class MedicalScribeAgentTest {

    @Inject
    AgentRegistry registry;

    @Test
    void usesExplicitIdAndName() {
        var d = registry.findById("hipaa-medical-scribe", "default").orElseThrow();
        assertThat(d.agentId()).isEqualTo("hipaa-medical-scribe");
        assertThat(d.name()).isEqualTo("HIPAA Medical Scribe");
    }

    @Test
    void hasComplianceFields() {
        var d = registry.findById("hipaa-medical-scribe", "default").orElseThrow();
        assertThat(d.jurisdiction()).isEqualTo("US");
        assertThat(d.dataHandlingPolicy()).isEqualTo("hipaa-compliant");
        assertThat(d.version()).isEqualTo("2.1.0");
    }

    @Test
    void hasGoalsWithCapabilityMapping() {
        var d = registry.findById("hipaa-medical-scribe", "default").orElseThrow();
        assertThat(d.goals()).hasSize(4);

        var transcription = d.goals().stream()
            .filter(g -> g.name().equals("accurate-transcription")).findFirst().orElseThrow();
        assertThat(transcription.priority()).isEqualTo(GoalPriority.PRIMARY);
        assertThat(transcription.capabilities()).containsExactly("transcription");

        var coding = d.goals().stream()
            .filter(g -> g.name().equals("icd-compliance")).findFirst().orElseThrow();
        assertThat(coding.capabilities()).containsExactly("clinical-coding");

        var efficiency = d.goals().stream()
            .filter(g -> g.name().equals("clinician-efficiency")).findFirst().orElseThrow();
        assertThat(efficiency.priority()).isEqualTo(GoalPriority.SECONDARY);
        assertThat(efficiency.capabilities()).isEmpty();
    }

    @Test
    void privateGoalExcludedFromPublicView() {
        var d = registry.findById("hipaa-medical-scribe", "default").orElseThrow();
        assertThat(d.publicGoals()).hasSize(3);
        assertThat(d.publicGoals().stream().map(g -> g.name()).toList())
            .doesNotContain("detect-safety-signals");

        var privateGoal = d.goals().stream()
            .filter(g -> g.name().equals("detect-safety-signals")).findFirst().orElseThrow();
        assertThat(privateGoal.visibility()).isEqualTo(Visibility.PRIVATE);
    }

    @Test
    void hasConstraintsWithSeverityAndVisibility() {
        var d = registry.findById("hipaa-medical-scribe", "default").orElseThrow();
        assertThat(d.constraints()).hasSize(3);

        var hard = d.constraints().stream()
            .filter(c -> c.name().equals("no-clinical-decisions")).findFirst().orElseThrow();
        assertThat(hard.severity()).isEqualTo(ConstraintSeverity.HARD);

        assertThat(d.publicConstraints()).hasSize(2);
        assertThat(d.publicConstraints().stream().map(c -> c.name()).toList())
            .doesNotContain("audit-trail");
    }
}

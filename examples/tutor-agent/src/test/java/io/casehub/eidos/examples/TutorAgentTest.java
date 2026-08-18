package io.casehub.eidos.examples;

import io.casehub.eidos.api.AgentRegistry;
import io.casehub.eidos.api.ConstraintSeverity;
import io.casehub.eidos.api.DispositionAxis;
import io.casehub.eidos.api.DispositionValue;
import io.casehub.eidos.api.GoalPriority;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class TutorAgentTest {

    @Inject
    AgentRegistry registry;

    @Test
    void hasDispositionAxesAndProfile() {
        var d = registry.findById("tutor-agent", "default").orElseThrow();
        assertThat(d.disposition().primaryTerm(DispositionAxis.SOCIAL_ORIENTATION)).isEqualTo("supportive");
        assertThat(d.disposition().primaryTerm(DispositionAxis.AUTONOMY)).isEqualTo("collaborative");
        assertThat(d.disposition().primaryTerm(DispositionAxis.CONFLICT_MODE)).isEqualTo("compromising");
        assertThat(d.disposition().dispositionProfile())
            .containsExactly(DispositionValue.of("INTROVERTED_SENSING"), DispositionValue.of("EXTRAVERTED_FEELING"));
    }

    @Test
    void goalWithMultiCapabilityMapping() {
        var d = registry.findById("tutor-agent", "default").orElseThrow();
        var outcomes = d.goals().stream()
            .filter(g -> g.name().equals("learning-outcomes")).findFirst().orElseThrow();
        assertThat(outcomes.priority()).isEqualTo(GoalPriority.PRIMARY);
        assertThat(outcomes.capabilities()).containsExactly("explanation", "assessment");
    }

    @Test
    void crossCuttingGoalHasNoCapabilities() {
        var d = registry.findById("tutor-agent", "default").orElseThrow();
        var engagement = d.goals().stream()
            .filter(g -> g.name().equals("engagement")).findFirst().orElseThrow();
        assertThat(engagement.priority()).isEqualTo(GoalPriority.SECONDARY);
        assertThat(engagement.capabilities()).isEmpty();
    }

    @Test
    void hasHardAndSoftConstraints() {
        var d = registry.findById("tutor-agent", "default").orElseThrow();
        assertThat(d.constraints()).hasSize(2);

        var hard = d.constraints().stream()
            .filter(c -> c.name().equals("age-appropriate")).findFirst().orElseThrow();
        assertThat(hard.severity()).isEqualTo(ConstraintSeverity.HARD);

        var soft = d.constraints().stream()
            .filter(c -> c.name().equals("no-answer-giving")).findFirst().orElseThrow();
        assertThat(soft.severity()).isEqualTo(ConstraintSeverity.SOFT);
    }
}

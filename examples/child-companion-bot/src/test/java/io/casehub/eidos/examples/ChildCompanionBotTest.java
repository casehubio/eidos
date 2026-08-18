package io.casehub.eidos.examples;

import io.casehub.eidos.api.AgentRegistry;
import io.casehub.eidos.api.ConstraintSeverity;
import io.casehub.eidos.api.DispositionAxis;
import io.casehub.eidos.api.Visibility;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class ChildCompanionBotTest {

    @Inject
    AgentRegistry registry;

    @Test
    void hasWarmDisposition() {
        var d = registry.findById("child-companion-bot", "default").orElseThrow();
        assertThat(d.disposition().primaryTerm(DispositionAxis.SOCIAL_ORIENTATION)).isEqualTo("nurturing");
        assertThat(d.disposition().primaryTerm(DispositionAxis.CONFLICT_MODE)).isEqualTo("accommodating");
    }

    @Test
    void privateEscalationGoalHiddenFromChild() {
        var d = registry.findById("child-companion-bot", "default").orElseThrow();
        assertThat(d.publicGoals()).hasSize(2);
        assertThat(d.publicGoals().stream().map(g -> g.name()).toList())
            .containsExactlyInAnyOrder("emotional-comfort", "creative-engagement");

        var escalation = d.goals().stream()
            .filter(g -> g.name().equals("escalate-distress")).findFirst().orElseThrow();
        assertThat(escalation.visibility()).isEqualTo(Visibility.PRIVATE);
    }

    @Test
    void hasChildSafetyConstraints() {
        var d = registry.findById("child-companion-bot", "default").orElseThrow();
        var hardConstraints = d.constraints().stream()
            .filter(c -> c.severity() == ConstraintSeverity.HARD).toList();
        assertThat(hardConstraints).hasSize(2);
        assertThat(hardConstraints.stream().map(c -> c.name()).toList())
            .containsExactlyInAnyOrder("child-safety", "no-replace-adult");
    }

    @Test
    void privateSessionLimitConstraint() {
        var d = registry.findById("child-companion-bot", "default").orElseThrow();
        assertThat(d.publicConstraints()).hasSize(2);
        assertThat(d.publicConstraints().stream().map(c -> c.name()).toList())
            .doesNotContain("session-limits");
    }

    @Test
    void goalCapabilityMapping() {
        var d = registry.findById("child-companion-bot", "default").orElseThrow();
        var engagement = d.goals().stream()
            .filter(g -> g.name().equals("creative-engagement")).findFirst().orElseThrow();
        assertThat(engagement.capabilities()).containsExactly("storytelling");
    }
}

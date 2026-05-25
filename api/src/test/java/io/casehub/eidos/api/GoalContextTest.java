package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class GoalContextTest {

    @Test
    void of_creates_goal_with_empty_subgoals_and_null_case_ref() {
        final var goal = GoalContext.of("review this PR");
        assertThat(goal.description()).isEqualTo("review this PR");
        assertThat(goal.subGoals()).isEmpty();
        assertThat(goal.caseRef()).isNull();
    }

    @Test
    void full_constructor_preserves_all_fields() {
        final var goal = new GoalContext("review", List.of("check style", "check tests"), "case-123");
        assertThat(goal.description()).isEqualTo("review");
        assertThat(goal.subGoals()).containsExactly("check style", "check tests");
        assertThat(goal.caseRef()).isEqualTo("case-123");
    }
}

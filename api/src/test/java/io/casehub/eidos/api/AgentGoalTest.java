package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class AgentGoalTest {

    @Test
    void valid_goal_constructs_successfully() {
        var goal = new AgentGoal("find-diamond", "Find the Doily Diamond",
            GoalPriority.PRIMARY, Visibility.PUBLIC);
        assertThat(goal.name()).isEqualTo("find-diamond");
        assertThat(goal.description()).isEqualTo("Find the Doily Diamond");
        assertThat(goal.priority()).isEqualTo(GoalPriority.PRIMARY);
        assertThat(goal.visibility()).isEqualTo(Visibility.PUBLIC);
    }

    @Test void null_name_throws() {
        assertThatThrownBy(() -> new AgentGoal(null, "desc", GoalPriority.PRIMARY, Visibility.PUBLIC))
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName()).isEqualTo("goal.name"));
    }

    @Test void blank_name_throws() {
        assertThatThrownBy(() -> new AgentGoal("  ", "desc", GoalPriority.PRIMARY, Visibility.PUBLIC))
            .isInstanceOf(AgentValidationException.class);
    }

    @Test void name_exceeds_100_throws() {
        assertThatThrownBy(() -> new AgentGoal("a".repeat(101), "desc", GoalPriority.PRIMARY, Visibility.PUBLIC))
            .isInstanceOf(AgentValidationException.class);
    }

    @Test void name_at_100_accepted() {
        assertThatNoException().isThrownBy(
            () -> new AgentGoal("a".repeat(100), "desc", GoalPriority.PRIMARY, Visibility.PUBLIC));
    }

    @Test void null_description_throws() {
        assertThatThrownBy(() -> new AgentGoal("g", null, GoalPriority.PRIMARY, Visibility.PUBLIC))
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName()).isEqualTo("goal.description"));
    }

    @Test void description_exceeds_500_throws() {
        assertThatThrownBy(() -> new AgentGoal("g", "d".repeat(501), GoalPriority.PRIMARY, Visibility.PUBLIC))
            .isInstanceOf(AgentValidationException.class);
    }

    @Test void null_priority_throws() {
        assertThatThrownBy(() -> new AgentGoal("g", "d", null, Visibility.PUBLIC))
            .isInstanceOf(NullPointerException.class);
    }

    @Test void null_visibility_throws() {
        assertThatThrownBy(() -> new AgentGoal("g", "d", GoalPriority.PRIMARY, null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test void name_with_control_char_throws() {
        assertThatThrownBy(() -> new AgentGoal("goal\ttab", "desc", GoalPriority.PRIMARY, Visibility.PUBLIC))
            .isInstanceOf(AgentValidationException.class);
    }

    @Test void description_with_bidi_control_throws() {
        assertThatThrownBy(() -> new AgentGoal("g", "desc‏hidden", GoalPriority.PRIMARY, Visibility.PUBLIC))
            .isInstanceOf(AgentValidationException.class);
    }
}

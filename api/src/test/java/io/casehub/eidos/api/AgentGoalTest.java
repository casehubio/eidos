package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentGoalTest {

    @Test
    void valid_goal_constructs_successfully() {
        var goal = new AgentGoal("find-diamond", "Find the Doily Diamond",
            GoalPriority.PRIMARY, Visibility.PUBLIC, List.of());
        assertThat(goal.name()).isEqualTo("find-diamond");
        assertThat(goal.description()).isEqualTo("Find the Doily Diamond");
        assertThat(goal.priority()).isEqualTo(GoalPriority.PRIMARY);
        assertThat(goal.visibility()).isEqualTo(Visibility.PUBLIC);
    }

    @Test void null_name_throws() {
        assertThatThrownBy(() -> new AgentGoal(null, "desc", GoalPriority.PRIMARY, Visibility.PUBLIC, List.of()))
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName()).isEqualTo("goal.name"));
    }

    @Test void blank_name_throws() {
        assertThatThrownBy(() -> new AgentGoal("  ", "desc", GoalPriority.PRIMARY, Visibility.PUBLIC, List.of()))
            .isInstanceOf(AgentValidationException.class);
    }

    @Test void name_exceeds_100_throws() {
        assertThatThrownBy(() -> new AgentGoal("a".repeat(101), "desc", GoalPriority.PRIMARY, Visibility.PUBLIC, List.of()))
            .isInstanceOf(AgentValidationException.class);
    }

    @Test void name_at_100_accepted() {
        assertThatNoException().isThrownBy(
            () -> new AgentGoal("a".repeat(100), "desc", GoalPriority.PRIMARY, Visibility.PUBLIC, List.of()));
    }

    @Test void null_description_throws() {
        assertThatThrownBy(() -> new AgentGoal("g", null, GoalPriority.PRIMARY, Visibility.PUBLIC, List.of()))
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName()).isEqualTo("goal.description"));
    }

    @Test void description_exceeds_500_throws() {
        assertThatThrownBy(() -> new AgentGoal("g", "d".repeat(501), GoalPriority.PRIMARY, Visibility.PUBLIC, List.of()))
            .isInstanceOf(AgentValidationException.class);
    }

    @Test void null_priority_throws() {
        assertThatThrownBy(() -> new AgentGoal("g", "d", null, Visibility.PUBLIC, List.of()))
            .isInstanceOf(NullPointerException.class);
    }

    @Test void null_visibility_throws() {
        assertThatThrownBy(() -> new AgentGoal("g", "d", GoalPriority.PRIMARY, null, List.of()))
            .isInstanceOf(NullPointerException.class);
    }

    @Test void name_with_control_char_throws() {
        assertThatThrownBy(() -> new AgentGoal("goal	tab", "desc", GoalPriority.PRIMARY, Visibility.PUBLIC, List.of()))
            .isInstanceOf(AgentValidationException.class);
    }

    @Test void description_with_bidi_control_throws() {
        assertThatThrownBy(() -> new AgentGoal("g", "desc‏hidden", GoalPriority.PRIMARY, Visibility.PUBLIC, List.of()))
            .isInstanceOf(AgentValidationException.class);
    }

    @Test
    void capabilities_defaults_to_empty_when_null() {
        var goal = new AgentGoal("g", "desc", GoalPriority.PRIMARY, Visibility.PUBLIC, null);
        assertThat(goal.capabilities()).isEmpty();
    }

    @Test
    void capabilities_preserved() {
        var goal = new AgentGoal("g", "desc", GoalPriority.PRIMARY, Visibility.PUBLIC,
                                 List.of("code-review", "testing"));
        assertThat(goal.capabilities()).containsExactly("code-review", "testing");
    }

    @Test
    void capabilities_immutable() {
        var caps = new java.util.ArrayList<>(List.of("a"));
        var goal = new AgentGoal("g", "desc", GoalPriority.PRIMARY, Visibility.PUBLIC, caps);
        assertThatThrownBy(() -> goal.capabilities().add("b"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void capabilities_null_elements_filtered() {
        var goal = new AgentGoal("g", "desc", GoalPriority.PRIMARY, Visibility.PUBLIC,
                                 java.util.Arrays.asList("a", null, "b"));
        assertThat(goal.capabilities()).containsExactly("a", "b");
    }

    @Test
    void capabilities_duplicate_throws() {
        assertThatThrownBy(() -> new AgentGoal("g", "desc", GoalPriority.PRIMARY, Visibility.PUBLIC,
                                               List.of("cap-a", "cap-a")))
                .isInstanceOf(AgentValidationException.class)
                .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName())
                                         .isEqualTo("goal.capabilities"));
    }

    @Test
    void capabilities_name_exceeds_max_throws() {
        assertThatThrownBy(() -> new AgentGoal("g", "desc", GoalPriority.PRIMARY, Visibility.PUBLIC,
                                               List.of("a".repeat(101))))
                .isInstanceOf(AgentValidationException.class);
    }
}

package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AgentGoalBuilderTest {

    @Test
    void toBuilder_preservesAllFields() {
        var goal = new AgentGoal("g1", "desc", GoalPriority.PRIMARY,
                                  Visibility.PUBLIC, List.of("cap-1"), null);
        var copy = goal.toBuilder().build();
        assertEquals(goal, copy);
    }

    @Test
    void toBuilder_changesDescription() {
        var goal = new AgentGoal("g1", "old desc", GoalPriority.PRIMARY,
                                  Visibility.PUBLIC, List.of(), null);
        var revised = goal.toBuilder().description("new desc").build();
        assertEquals("new desc", revised.description());
        assertEquals(goal.name(), revised.name());
        assertEquals(goal.priority(), revised.priority());
        assertEquals(goal.visibility(), revised.visibility());
    }

    @Test
    void toBuilder_changesPriority() {
        var goal = new AgentGoal("g1", "desc", GoalPriority.SECONDARY,
                                  Visibility.PUBLIC, List.of(), null);
        var revised = goal.toBuilder().priority(GoalPriority.PRIMARY).build();
        assertEquals(GoalPriority.PRIMARY, revised.priority());
        assertEquals("desc", revised.description());
    }

    @Test
    void toBuilder_changesMultipleFields() {
        var goal = new AgentGoal("g1", "old", GoalPriority.SECONDARY,
                                  Visibility.PRIVATE, List.of("a"), null);
        var revised = goal.toBuilder()
            .description("new")
            .priority(GoalPriority.PRIMARY)
            .visibility(Visibility.PUBLIC)
            .build();
        assertEquals("new", revised.description());
        assertEquals(GoalPriority.PRIMARY, revised.priority());
        assertEquals(Visibility.PUBLIC, revised.visibility());
        assertEquals("g1", revised.name());
    }
}

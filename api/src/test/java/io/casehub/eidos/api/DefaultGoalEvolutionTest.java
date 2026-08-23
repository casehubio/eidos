package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DefaultGoalEvolutionTest {

    private final DefaultGoalEvolution evolution = new DefaultGoalEvolution();

    private AgentDescriptor descriptorWithGoals(AgentGoal... goals) {
        return AgentDescriptor.builder()
            .agentId("agent-1").name("test-agent").slot("default")
            .tenancyId("tenant-1").goals(List.of(goals)).build();
    }

    private AgentGoal goal(String name, GoalPriority priority) {
        return new AgentGoal(name, "description for " + name, priority,
                             Visibility.PUBLIC, List.of(), null);
    }

    @Test
    void promotesSecondaryOnHighSuccessRate() {
        var descriptor = descriptorWithGoals(goal("g1", GoalPriority.SECONDARY));
        var counts = Map.of("g1", new GoalOutcomeCounts(9, 1));
        var result = evolution.evaluate(descriptor, counts);
        assertInstanceOf(GoalEvolutionResult.Evolved.class, result);
        var evolved = (GoalEvolutionResult.Evolved) result;
        assertTrue(evolved.promotedGoals().contains("g1"));
        assertEquals(GoalPriority.PRIMARY,
            evolved.newGoals().stream().filter(g -> g.name().equals("g1"))
                   .findFirst().orElseThrow().priority());
    }

    @Test
    void demotesPrimaryOnHighFailureRate() {
        var descriptor = descriptorWithGoals(goal("g1", GoalPriority.PRIMARY));
        var counts = Map.of("g1", new GoalOutcomeCounts(2, 8));
        var result = evolution.evaluate(descriptor, counts);
        assertInstanceOf(GoalEvolutionResult.Evolved.class, result);
        var evolved = (GoalEvolutionResult.Evolved) result;
        assertTrue(evolved.demotedGoals().contains("g1"));
        assertEquals(GoalPriority.SECONDARY,
            evolved.newGoals().stream().filter(g -> g.name().equals("g1"))
                   .findFirst().orElseThrow().priority());
    }

    @Test
    void unchangedWhenRatesBetweenThresholds() {
        var descriptor = descriptorWithGoals(goal("g1", GoalPriority.SECONDARY));
        var counts = Map.of("g1", new GoalOutcomeCounts(6, 4));
        var result = evolution.evaluate(descriptor, counts);
        assertInstanceOf(GoalEvolutionResult.Unchanged.class, result);
    }

    @Test
    void dampenedWhenBelowMinOutcomes() {
        var descriptor = descriptorWithGoals(goal("g1", GoalPriority.SECONDARY));
        var counts = Map.of("g1", new GoalOutcomeCounts(3, 0));
        var result = evolution.evaluate(descriptor, counts);
        assertInstanceOf(GoalEvolutionResult.Dampened.class, result);
    }

    @Test
    void unchangedWhenNoOutcomes() {
        var descriptor = descriptorWithGoals(goal("g1", GoalPriority.SECONDARY));
        var result = evolution.evaluate(descriptor, Map.of());
        assertInstanceOf(GoalEvolutionResult.Unchanged.class, result);
    }

    @Test
    void unchangedWhenNoGoals() {
        var descriptor = AgentDescriptor.builder()
            .agentId("agent-1").name("test-agent").slot("default")
            .tenancyId("tenant-1").build();
        var result = evolution.evaluate(descriptor, Map.of());
        assertInstanceOf(GoalEvolutionResult.Unchanged.class, result);
    }

    @Test
    void multipleGoals_mixedOutcomes() {
        var descriptor = descriptorWithGoals(
            goal("promote-me", GoalPriority.SECONDARY),
            goal("demote-me", GoalPriority.PRIMARY),
            goal("leave-me", GoalPriority.SECONDARY));
        var counts = Map.of(
            "promote-me", new GoalOutcomeCounts(9, 1),
            "demote-me", new GoalOutcomeCounts(2, 8),
            "leave-me", new GoalOutcomeCounts(5, 5));
        var result = evolution.evaluate(descriptor, counts);
        assertInstanceOf(GoalEvolutionResult.Evolved.class, result);
        var evolved = (GoalEvolutionResult.Evolved) result;
        assertTrue(evolved.promotedGoals().contains("promote-me"));
        assertTrue(evolved.demotedGoals().contains("demote-me"));
        assertFalse(evolved.promotedGoals().contains("leave-me"));
        assertFalse(evolved.demotedGoals().contains("leave-me"));
    }

    @Test
    void promotionPreservesDescription() {
        var original = new AgentGoal("g1", "my description", GoalPriority.SECONDARY,
                                      Visibility.PUBLIC, List.of("cap-a"), null);
        var descriptor = descriptorWithGoals(original);
        var counts = Map.of("g1", new GoalOutcomeCounts(9, 1));
        var result = evolution.evaluate(descriptor, counts);
        var evolved = (GoalEvolutionResult.Evolved) result;
        var revised = evolved.newGoals().stream().filter(g -> g.name().equals("g1")).findFirst().orElseThrow();
        assertEquals("my description", revised.description());
        assertEquals(List.of("cap-a"), revised.capabilities());
        assertEquals(Visibility.PUBLIC, revised.visibility());
    }

    @Test
    void alreadyPrimary_highSuccess_noChange() {
        var descriptor = descriptorWithGoals(goal("g1", GoalPriority.PRIMARY));
        var counts = Map.of("g1", new GoalOutcomeCounts(9, 1));
        var result = evolution.evaluate(descriptor, counts);
        assertInstanceOf(GoalEvolutionResult.Unchanged.class, result);
    }

    @Test
    void alreadySecondary_highFailure_noChange() {
        var descriptor = descriptorWithGoals(goal("g1", GoalPriority.SECONDARY));
        var counts = Map.of("g1", new GoalOutcomeCounts(2, 8));
        var result = evolution.evaluate(descriptor, counts);
        assertInstanceOf(GoalEvolutionResult.Unchanged.class, result);
    }
}

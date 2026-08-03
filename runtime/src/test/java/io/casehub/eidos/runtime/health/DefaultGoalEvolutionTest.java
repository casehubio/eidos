package io.casehub.eidos.runtime.health;

import io.casehub.eidos.api.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DefaultGoalEvolutionTest {

    private DefaultGoalEvolution evolution;

    @BeforeEach
    void setUp() {
        evolution = new DefaultGoalEvolution();
    }

    @Test
    void noSignals_returnsUnchanged() {
        var descriptor = descriptorWithGoals(
            new AgentGoal("deliver", "Deliver results", GoalPriority.PRIMARY, Visibility.PUBLIC));
        var result = evolution.evaluate(descriptor, Map.of());
        assertInstanceOf(GoalEvolutionResult.Unchanged.class, result);
    }

    @Test
    void emptyGoals_returnsUnchanged() {
        var descriptor = AgentDescriptor.builder()
            .agentId("a1").name("Agent").tenancyId("t1").slot("tester")
            .goals(List.of())
            .build();
        var result = evolution.evaluate(descriptor, Map.of("x", new GoalOutcomeCounts(20, 0)));
        assertInstanceOf(GoalEvolutionResult.Unchanged.class, result);
    }

    @Test
    void secondaryExceedsPromotionThreshold_promotes() {
        var descriptor = descriptorWithGoals(
            new AgentGoal("primary", "Primary", GoalPriority.PRIMARY, Visibility.PUBLIC),
            new AgentGoal("rising", "Rising", GoalPriority.SECONDARY, Visibility.PUBLIC));
        var counts = Map.of("rising", new GoalOutcomeCounts(15, 1));
        var result = evolution.evaluate(descriptor, counts);
        assertInstanceOf(GoalEvolutionResult.Evolved.class, result);
        var evolved = (GoalEvolutionResult.Evolved) result;
        assertTrue(evolved.promotedGoals().contains("rising"));
        assertTrue(evolved.newGoals().stream()
            .filter(g -> g.name().equals("rising"))
            .allMatch(g -> g.priority() == GoalPriority.PRIMARY));
    }

    @Test
    void primaryExceedsDemotionThreshold_demotes() {
        var descriptor = descriptorWithGoals(
            new AgentGoal("failing", "Failing", GoalPriority.PRIMARY, Visibility.PUBLIC),
            new AgentGoal("backup", "Backup", GoalPriority.PRIMARY, Visibility.PUBLIC));
        var counts = Map.of("failing", new GoalOutcomeCounts(2, 15));
        var result = evolution.evaluate(descriptor, counts);
        assertInstanceOf(GoalEvolutionResult.Evolved.class, result);
        var evolved = (GoalEvolutionResult.Evolved) result;
        assertTrue(evolved.demotedGoals().contains("failing"));
    }

    @Test
    void lastPrimaryDemotion_swapsWithBestSecondary() {
        var descriptor = descriptorWithGoals(
            new AgentGoal("failing", "Failing", GoalPriority.PRIMARY, Visibility.PUBLIC),
            new AgentGoal("rising", "Rising", GoalPriority.SECONDARY, Visibility.PUBLIC));
        var counts = Map.of(
            "failing", new GoalOutcomeCounts(2, 15),
            "rising", new GoalOutcomeCounts(8, 2));
        var result = evolution.evaluate(descriptor, counts);
        assertInstanceOf(GoalEvolutionResult.Evolved.class, result);
        var evolved = (GoalEvolutionResult.Evolved) result;
        assertTrue(evolved.promotedGoals().contains("rising"));
        assertTrue(evolved.demotedGoals().contains("failing"));
        assertTrue(evolved.newGoals().stream()
            .anyMatch(g -> g.priority() == GoalPriority.PRIMARY));
    }

    @Test
    void lastPrimaryDemotion_noSecondaryAvailable_returnsDampened() {
        var descriptor = descriptorWithGoals(
            new AgentGoal("failing", "Failing", GoalPriority.PRIMARY, Visibility.PUBLIC));
        var counts = Map.of("failing", new GoalOutcomeCounts(2, 15));
        var result = evolution.evaluate(descriptor, counts);
        assertInstanceOf(GoalEvolutionResult.Dampened.class, result);
    }

    @Test
    void belowMinCount_returnsUnchanged() {
        var descriptor = descriptorWithGoals(
            new AgentGoal("primary", "Primary", GoalPriority.PRIMARY, Visibility.PUBLIC),
            new AgentGoal("rising", "Rising", GoalPriority.SECONDARY, Visibility.PUBLIC));
        var counts = Map.of("rising", new GoalOutcomeCounts(5, 0));
        var result = evolution.evaluate(descriptor, counts);
        assertInstanceOf(GoalEvolutionResult.Unchanged.class, result);
    }

    @Test
    void belowPromotionRate_returnsUnchanged() {
        var descriptor = descriptorWithGoals(
            new AgentGoal("primary", "Primary", GoalPriority.PRIMARY, Visibility.PUBLIC),
            new AgentGoal("mediocre", "Mediocre", GoalPriority.SECONDARY, Visibility.PUBLIC));
        var counts = Map.of("mediocre", new GoalOutcomeCounts(12, 8));
        var result = evolution.evaluate(descriptor, counts);
        assertInstanceOf(GoalEvolutionResult.Unchanged.class, result);
    }

    private AgentDescriptor descriptorWithGoals(AgentGoal... goals) {
        return AgentDescriptor.builder()
            .agentId("a1").name("Agent").tenancyId("t1").slot("tester")
            .goals(List.of(goals))
            .build();
    }
}

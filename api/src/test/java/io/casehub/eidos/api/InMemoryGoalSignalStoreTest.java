package io.casehub.eidos.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryGoalSignalStoreTest {

    private InMemoryGoalSignalStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryGoalSignalStore();
    }

    @Test
    void recordOutcome_incrementsSuccessCount() {
        store.recordOutcome("agent-1", "tenant-1", "goal-a", GoalOutcome.SUCCESS);
        store.recordOutcome("agent-1", "tenant-1", "goal-a", GoalOutcome.SUCCESS);
        Map<String, GoalOutcomeCounts> counts = store.outcomeCounts("agent-1", "tenant-1");
        assertEquals(2, counts.get("goal-a").successCount());
        assertEquals(0, counts.get("goal-a").failureCount());
    }

    @Test
    void recordOutcome_incrementsFailureCount() {
        store.recordOutcome("agent-1", "tenant-1", "goal-a", GoalOutcome.FAILURE);
        Map<String, GoalOutcomeCounts> counts = store.outcomeCounts("agent-1", "tenant-1");
        assertEquals(0, counts.get("goal-a").successCount());
        assertEquals(1, counts.get("goal-a").failureCount());
    }

    @Test
    void outcomeCounts_emptyWhenNoRecords() {
        assertTrue(store.outcomeCounts("agent-1", "tenant-1").isEmpty());
    }

    @Test
    void outcomeCounts_isolatedByAgentAndTenancy() {
        store.recordOutcome("agent-1", "tenant-1", "goal-a", GoalOutcome.SUCCESS);
        store.recordOutcome("agent-2", "tenant-1", "goal-a", GoalOutcome.FAILURE);
        assertEquals(1, store.outcomeCounts("agent-1", "tenant-1").get("goal-a").successCount());
        assertEquals(0, store.outcomeCounts("agent-1", "tenant-1").get("goal-a").failureCount());
        assertEquals(0, store.outcomeCounts("agent-2", "tenant-1").get("goal-a").successCount());
        assertEquals(1, store.outcomeCounts("agent-2", "tenant-1").get("goal-a").failureCount());
    }

    @Test
    void clear_resetsAllCounts() {
        store.recordOutcome("agent-1", "tenant-1", "goal-a", GoalOutcome.SUCCESS);
        store.recordOutcome("agent-1", "tenant-1", "goal-b", GoalOutcome.FAILURE);
        store.clear("agent-1", "tenant-1");
        assertTrue(store.outcomeCounts("agent-1", "tenant-1").isEmpty());
    }

    @Test
    void clear_doesNotAffectOtherAgents() {
        store.recordOutcome("agent-1", "tenant-1", "goal-a", GoalOutcome.SUCCESS);
        store.recordOutcome("agent-2", "tenant-1", "goal-a", GoalOutcome.SUCCESS);
        store.clear("agent-1", "tenant-1");
        assertEquals(1, store.outcomeCounts("agent-2", "tenant-1").get("goal-a").successCount());
    }

    @Test
    void decay_reducesCounts() {
        for (int i = 0; i < 10; i++) {
            store.recordOutcome("agent-1", "tenant-1", "goal-a", GoalOutcome.SUCCESS);
        }
        for (int i = 0; i < 4; i++) {
            store.recordOutcome("agent-1", "tenant-1", "goal-a", GoalOutcome.FAILURE);
        }
        store.decay("agent-1", "tenant-1", 0.5);
        Map<String, GoalOutcomeCounts> counts = store.outcomeCounts("agent-1", "tenant-1");
        assertEquals(5, counts.get("goal-a").successCount());
        assertEquals(2, counts.get("goal-a").failureCount());
    }

    @Test
    void decay_removesZeroCountGoals() {
        store.recordOutcome("agent-1", "tenant-1", "goal-a", GoalOutcome.SUCCESS);
        store.decay("agent-1", "tenant-1", 0.0);
        assertTrue(store.outcomeCounts("agent-1", "tenant-1").isEmpty());
    }

    @Test
    void multipleGoals_trackedIndependently() {
        store.recordOutcome("agent-1", "tenant-1", "goal-a", GoalOutcome.SUCCESS);
        store.recordOutcome("agent-1", "tenant-1", "goal-b", GoalOutcome.FAILURE);
        Map<String, GoalOutcomeCounts> counts = store.outcomeCounts("agent-1", "tenant-1");
        assertEquals(1, counts.get("goal-a").successCount());
        assertEquals(0, counts.get("goal-a").failureCount());
        assertEquals(0, counts.get("goal-b").successCount());
        assertEquals(1, counts.get("goal-b").failureCount());
    }

    @Test
    void outcomeCounts_returnsUnmodifiableMap() {
        store.recordOutcome("agent-1", "tenant-1", "goal-a", GoalOutcome.SUCCESS);
        Map<String, GoalOutcomeCounts> counts = store.outcomeCounts("agent-1", "tenant-1");
        assertThrows(UnsupportedOperationException.class,
            () -> counts.put("goal-x", new GoalOutcomeCounts(1, 0)));
    }
}

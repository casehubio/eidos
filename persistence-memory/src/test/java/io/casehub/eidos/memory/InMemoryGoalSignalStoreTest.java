package io.casehub.eidos.memory;

import io.casehub.eidos.api.GoalOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryGoalSignalStoreTest {

    private InMemoryGoalSignalStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryGoalSignalStore();
    }

    @Test
    void recordAndRetrieve() {
        store.recordOutcome("a1", "t1", "deliver", GoalOutcome.SUCCESS);
        store.recordOutcome("a1", "t1", "deliver", GoalOutcome.SUCCESS);
        store.recordOutcome("a1", "t1", "deliver", GoalOutcome.FAILURE);
        var counts = store.outcomeCounts("a1", "t1");
        assertEquals(2, counts.get("deliver").successCount());
        assertEquals(1, counts.get("deliver").failureCount());
    }

    @Test
    void multipleGoals_tracked_independently() {
        store.recordOutcome("a1", "t1", "goal-a", GoalOutcome.SUCCESS);
        store.recordOutcome("a1", "t1", "goal-b", GoalOutcome.FAILURE);
        var counts = store.outcomeCounts("a1", "t1");
        assertEquals(1, counts.get("goal-a").successCount());
        assertEquals(0, counts.get("goal-a").failureCount());
        assertEquals(0, counts.get("goal-b").successCount());
        assertEquals(1, counts.get("goal-b").failureCount());
    }

    @Test
    void differentAgents_isolated() {
        store.recordOutcome("a1", "t1", "goal", GoalOutcome.SUCCESS);
        store.recordOutcome("a2", "t1", "goal", GoalOutcome.FAILURE);
        assertEquals(1, store.outcomeCounts("a1", "t1").get("goal").successCount());
        assertEquals(1, store.outcomeCounts("a2", "t1").get("goal").failureCount());
    }

    @Test
    void decay_reducesCountsByFactor() {
        for (int i = 0; i < 10; i++) {
            store.recordOutcome("a1", "t1", "goal", GoalOutcome.SUCCESS);
        }
        for (int i = 0; i < 5; i++) {
            store.recordOutcome("a1", "t1", "goal", GoalOutcome.FAILURE);
        }
        store.decay("a1", "t1", 0.20);
        var counts = store.outcomeCounts("a1", "t1").get("goal");
        assertEquals(8, counts.successCount());
        assertEquals(4, counts.failureCount());
    }

    @Test
    void clear_removesAllForAgent() {
        store.recordOutcome("a1", "t1", "goal", GoalOutcome.SUCCESS);
        store.recordOutcome("a2", "t1", "goal", GoalOutcome.SUCCESS);
        store.clear("a1", "t1");
        assertTrue(store.outcomeCounts("a1", "t1").isEmpty());
        assertFalse(store.outcomeCounts("a2", "t1").isEmpty());
    }

    @Test
    void emptyStore_returnsEmptyMap() {
        assertTrue(store.outcomeCounts("a1", "t1").isEmpty());
    }
}

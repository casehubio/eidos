package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GoalOutcomeCountsTest {

    @Test
    void successRate_calculatesCorrectly() {
        assertEquals(0.8, new GoalOutcomeCounts(8, 2).successRate(), 0.001);
    }

    @Test
    void successRate_zeroTotal_returnsZero() {
        assertEquals(0.0, new GoalOutcomeCounts(0, 0).successRate(), 0.001);
    }

    @Test
    void successRate_allSuccess_returnsOne() {
        assertEquals(1.0, new GoalOutcomeCounts(5, 0).successRate(), 0.001);
    }

    @Test
    void successRate_allFailure_returnsZero() {
        assertEquals(0.0, new GoalOutcomeCounts(0, 5).successRate(), 0.001);
    }

    @Test
    void negativeSuccessCount_throws() {
        assertThrows(IllegalArgumentException.class, () -> new GoalOutcomeCounts(-1, 0));
    }

    @Test
    void negativeFailureCount_throws() {
        assertThrows(IllegalArgumentException.class, () -> new GoalOutcomeCounts(0, -1));
    }
}

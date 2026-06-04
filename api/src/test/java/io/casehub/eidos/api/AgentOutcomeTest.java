package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentOutcomeTest {

    static final Instant NOW = Instant.parse("2026-06-04T10:00:00Z");

    static AgentOutcome valid() {
        return new AgentOutcome("task-1", TaskResult.SUCCEEDED, 0.9, NOW, null);
    }

    // ── taskId (required) ──────────────────────────────────────────────────────

    @Test
    void null_taskId_throws() {
        assertThatNullPointerException()
            .isThrownBy(() -> new AgentOutcome(null, TaskResult.SUCCEEDED, 0.9, NOW, null))
            .withMessageContaining("taskId");
    }

    // ── result (required) ─────────────────────────────────────────────────────

    @Test
    void null_result_throws() {
        assertThatNullPointerException()
            .isThrownBy(() -> new AgentOutcome("task-1", null, 0.9, NOW, null))
            .withMessageContaining("result");
    }

    // ── observedAt (required) ─────────────────────────────────────────────────

    @Test
    void null_observedAt_throws() {
        assertThatNullPointerException()
            .isThrownBy(() -> new AgentOutcome("task-1", TaskResult.SUCCEEDED, 0.9, null, null))
            .withMessageContaining("observedAt");
    }

    // ── confidence (0.0–1.0 inclusive) ────────────────────────────────────────

    @Test
    void confidence_below_zero_throws() {
        assertThatThrownBy(() -> new AgentOutcome("task-1", TaskResult.SUCCEEDED, -0.001, NOW, null))
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName())
                .isEqualTo("confidence"));
    }

    @Test
    void confidence_above_one_throws() {
        assertThatThrownBy(() -> new AgentOutcome("task-1", TaskResult.SUCCEEDED, 1.001, NOW, null))
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName())
                .isEqualTo("confidence"));
    }

    @Test
    void confidence_zero_is_valid() {
        assertThatNoException().isThrownBy(() ->
            new AgentOutcome("task-1", TaskResult.SUCCEEDED, 0.0, NOW, null));
    }

    @Test
    void confidence_one_is_valid() {
        assertThatNoException().isThrownBy(() ->
            new AgentOutcome("task-1", TaskResult.SUCCEEDED, 1.0, NOW, null));
    }

    @Test
    void nan_confidence_throws() {
        assertThatThrownBy(() -> new AgentOutcome("t", TaskResult.SUCCEEDED, Double.NaN, NOW, null))
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName())
                .isEqualTo("confidence"));
    }

    // ── valid construction ─────────────────────────────────────────────────────

    @Test
    void valid_outcome_constructs_cleanly() {
        assertThatNoException().isThrownBy(AgentOutcomeTest::valid);
    }

    @Test
    void valid_outcome_fields_equal_inputs() {
        var outcome = new AgentOutcome("task-42", TaskResult.FAILED, 0.3, NOW, DegradationReason.OVERLOADED);
        assertThat(outcome.taskId()).isEqualTo("task-42");
        assertThat(outcome.result()).isEqualTo(TaskResult.FAILED);
        assertThat(outcome.confidence()).isEqualTo(0.3);
        assertThat(outcome.observedAt()).isEqualTo(NOW);
        assertThat(outcome.degradationReason()).isEqualTo(DegradationReason.OVERLOADED);
    }
}

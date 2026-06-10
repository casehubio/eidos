package io.casehub.eidos.eval;

import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static io.casehub.eidos.api.DispositionAxis.RISK_APPETITE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BehavioralPairResultTest {

    static final VariantPair PAIR = new VariantPair(
        RISK_APPETITE, "sw-engineer-bold", "sw-engineer-careful", List.of());

    @Test
    void rejects_effectSize_below_1() {
        assertThatThrownBy(() -> new BehavioralPairResult(
            PAIR, "question?", "bold response", "careful response", true, 0, "reasoning"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("out of range");
    }

    @Test
    void rejects_effectSize_above_5() {
        assertThatThrownBy(() -> new BehavioralPairResult(
            PAIR, "question?", "bold response", "careful response", true, 6, "reasoning"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("out of range");
    }

    @Test
    void accepts_boundary_effectSize_values() {
        new BehavioralPairResult(PAIR, "q", "a", "b", true, 1, "r");
        new BehavioralPairResult(PAIR, "q", "a", "b", true, 5, "r");
    }

    @Test
    void correct_is_true_when_higher_identified() {
        final var result = new BehavioralPairResult(PAIR, "q", "a", "b", true, 3, "r");
        assertThat(result.correct()).isTrue();
        assertThat(result.pair()).isEqualTo(PAIR);
        assertThat(result.question()).isEqualTo("q");
    }

    @Test
    void behavioralReport_computes_accuracy_as_fraction_correct() {
        final var r1 = new BehavioralPairResult(PAIR, "q1", "a", "b", true, 3, "r");
        final var r2 = new BehavioralPairResult(PAIR, "q2", "a", "b", false, 2, "r");
        final var r3 = new BehavioralPairResult(PAIR, "q3", "a", "b", true, 4, "r");
        final var report = new BehavioralReport(Instant.now(), "claude", List.of(r1, r2, r3), 2.0 / 3);
        assertThat(report.accuracy()).isEqualTo(2.0 / 3);
        assertThat(report.modelLabel()).isEqualTo("claude");
        assertThat(report.results()).hasSize(3);
    }
}

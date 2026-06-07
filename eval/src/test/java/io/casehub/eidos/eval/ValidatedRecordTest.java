package io.casehub.eidos.eval;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentPromptContext;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidatedRecordTest {

    static SyntheticEvalCase minimalCase() {
        final var desc = new AgentDescriptor(
            "id", "N", null, null, null, null, null, null, null, null, null,
            "worker", List.of(), null, null, null, "t");
        return new SyntheticEvalCase("c", desc, AgentPromptContext.forFormat(RenderFormat.MARKDOWN));
    }

    @Test
    void proximityResult_rejects_score_below_0() {
        assertThatThrownBy(() -> new ProximityResult(minimalCase(), -1, "r", List.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("out of range");
    }

    @Test
    void proximityResult_rejects_score_above_5() {
        assertThatThrownBy(() -> new ProximityResult(minimalCase(), 6, "r", List.of()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void proximityResult_accepts_boundary_values() {
        new ProximityResult(minimalCase(), 0, "r", List.of());
        new ProximityResult(minimalCase(), 5, "r", List.of());
    }

    @Test
    void pairContrastResult_rejects_effectSize_below_1() {
        assertThatThrownBy(() -> new PairContrastResult("hi", "lo", "riskAppetite",
            RenderFormat.MARKDOWN, true, 0, "r"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("out of range");
    }

    @Test
    void pairContrastResult_rejects_effectSize_above_5() {
        assertThatThrownBy(() -> new PairContrastResult("hi", "lo", "riskAppetite",
            RenderFormat.MARKDOWN, true, 6, "r"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pairContrastResult_accepts_boundary_values() {
        new PairContrastResult("hi", "lo", "riskAppetite", RenderFormat.MARKDOWN, true, 1, "r");
        new PairContrastResult("hi", "lo", "riskAppetite", RenderFormat.MARKDOWN, true, 5, "r");
    }
}

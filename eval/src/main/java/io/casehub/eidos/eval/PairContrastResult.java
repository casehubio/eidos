package io.casehub.eidos.eval;

import io.casehub.eidos.api.DispositionAxis;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;

public record PairContrastResult(
    String profileHigh,
    String profileLow,
    DispositionAxis primaryAxis,
    RenderFormat format,
    boolean correctlyIdentified,
    int effectSize,
    String reasoning
) {
    public PairContrastResult {
        if (effectSize < 1 || effectSize > 5)
            throw new IllegalArgumentException("effectSize out of range: " + effectSize);
    }
}

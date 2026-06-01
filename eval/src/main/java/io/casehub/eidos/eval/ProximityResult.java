package io.casehub.eidos.eval;

import java.util.List;

public record ProximityResult(
    EvalCase evalCase,
    int score,
    String reasoning,
    List<String> gaps
) {
    public ProximityResult {
        if (score < 0 || score > 5)
            throw new IllegalArgumentException("ProximityResult score out of range: " + score);
    }
}

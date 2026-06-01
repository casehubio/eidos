package io.casehub.eidos.eval;

import io.casehub.eidos.api.SystemPromptRenderer.RenderedPrompt;

import java.util.List;
import java.util.Map;

public record EvalResult(
        EvalCase evalCase,
        RenderedPrompt rendered,
        boolean completenessPass,
        List<String> missingCapabilities,
        Map<EvalDimension, EvalScore> scores,
        double overall,      // 0.0–5.0; mean of applicable EvalDimension scores for the result's format
        List<String> issues
) {}

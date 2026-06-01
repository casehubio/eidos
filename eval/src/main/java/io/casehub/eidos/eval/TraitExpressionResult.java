package io.casehub.eidos.eval;

import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;

import java.util.Map;

public record TraitExpressionResult(
    ProfiledEvalCase evalCase,
    RenderFormat format,
    Map<String, Integer> expressionScores,
    Map<String, Boolean> directionMatches,
    String delegationAssessment
) {}

package io.casehub.eidos.eval;

public record AttributionDiagnosis(
    String profileName,
    String axis,
    int stage1Score,
    int stage2ExpressionScore,
    int stage3EffectSize,
    Attribution attribution
) {}

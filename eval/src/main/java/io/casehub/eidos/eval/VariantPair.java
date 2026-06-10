package io.casehub.eidos.eval;

import io.casehub.eidos.api.DispositionAxis;

import java.util.List;

public record VariantPair(
    DispositionAxis primaryAxis,
    String higher,
    String lower,
    List<String> scenarioQuestions
) {}

package io.casehub.eidos.eval;

import java.util.Map;

public record EvalSummary(
        boolean allCasesComplete,
        Map<EvalDimension, Double> meanByDimension,
        EvalDimension lowestScoringDimension, // ties broken by EvalDimension declaration order
        double meanOverall
) {}

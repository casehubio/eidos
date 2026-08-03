package io.casehub.eidos.eval;

import io.casehub.eidos.eval.FunctionActivationJudge.FunctionActivationResult;

import java.time.Instant;
import java.util.List;
import java.util.Map;

record BriefingExperimentReport(
    Instant timestamp,
    String modelLabel,
    List<ProfileResult> profiles,
    Map<BriefingCondition, ConditionSummary> aggregated,
    double frameworkContribution,
    double briefingContribution
) {
    record ProfileResult(
        String name,
        String mbtiType,
        String dominantFunction,
        String auxiliaryFunction,
        Map<BriefingCondition, FunctionActivationResult> conditions
    ) {}

    record ConditionSummary(double meanTaa) {}
}

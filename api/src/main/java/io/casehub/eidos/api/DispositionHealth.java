package io.casehub.eidos.api;

import java.util.Map;

public interface DispositionHealth {

    DispositionStatus probe(AgentDescriptor descriptor,
                            CapabilityHealth.ProbeContext context);

    sealed interface DispositionStatus
            permits DispositionStatus.Aligned,
                    DispositionStatus.Drifted,
                    DispositionStatus.EvolutionPending {

        record Aligned(Map<String, Double> effectiveWeights)
                implements DispositionStatus {}

        record Drifted(
                Map<String, Double> effectiveWeights,
                String mostActivated,
                double driftMagnitude)
                implements DispositionStatus {}

        record EvolutionPending(
                EvolutionType type,
                String candidateFunction,
                Map<String, Double> effectiveWeights)
                implements DispositionStatus {}
    }
}

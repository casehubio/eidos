package io.casehub.eidos.api;

import java.util.List;

public interface DispositionEvolution {

    EvolutionResult evaluate(AgentDescriptor descriptor,
                             DispositionHealth.DispositionStatus.EvolutionPending pending);

    sealed interface EvolutionResult
            permits EvolutionResult.Evolved,
                    EvolutionResult.Dampened {

        record Evolved(
                List<DispositionValue> newProfile,
                String previousTypeLabel,
                String newTypeLabel)
                implements EvolutionResult {}

        record Dampened(double decayFactor)
                implements EvolutionResult {}
    }
}

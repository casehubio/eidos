package io.casehub.eidos.core.health;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.DispositionEvolution;
import io.casehub.eidos.api.DispositionHealth.DispositionStatus;


public class NoOpDispositionEvolution implements DispositionEvolution {

    @Override
    public EvolutionResult evaluate(final AgentDescriptor descriptor,
                                    final DispositionStatus.EvolutionPending pending) {
        return new EvolutionResult.Dampened(0.0);
    }
}

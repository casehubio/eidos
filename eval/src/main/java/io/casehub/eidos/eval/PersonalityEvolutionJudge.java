package io.casehub.eidos.eval;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentDisposition;
import io.casehub.eidos.api.CapabilityHealth;
import io.casehub.eidos.api.DispositionEvolution;
import io.casehub.eidos.api.DispositionEvolution.EvolutionResult;
import io.casehub.eidos.api.DispositionHealth;
import io.casehub.eidos.api.DispositionHealth.DispositionStatus;
import io.casehub.eidos.api.DispositionSignalStore;
import io.casehub.eidos.api.DispositionValue;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Comparator;
import java.util.List;

@ApplicationScoped
public class PersonalityEvolutionJudge {

    static final double DOMINANT_MIN = 0.31;
    static final double AUXILIARY_MIN = 0.06;
    static final double AUXILIARY_MAX = 0.30;

    private final DispositionHealth health;
    private final DispositionEvolution evolution;
    private final DispositionSignalStore signalStore;

    @Inject
    public PersonalityEvolutionJudge(final DispositionHealth health,
                                      final DispositionEvolution evolution,
                                      final DispositionSignalStore signalStore) {
        this.health = health;
        this.evolution = evolution;
        this.signalStore = signalStore;
    }

    public EvolutionTestResult evaluate(final AgentDescriptor descriptor,
                                         final String targetFunction,
                                         final int activationCount) {
        signalStore.clear(descriptor.agentId(), descriptor.tenancyId());

        for (int i = 0; i < activationCount; i++) {
            signalStore.recordActivation(descriptor.agentId(), descriptor.tenancyId(),
                    targetFunction);
        }

        final var probeResult = health.probe(descriptor, CapabilityHealth.ProbeContext.of(null));

        if (!(probeResult instanceof DispositionStatus.EvolutionPending pending)) {
            return new EvolutionTestResult(
                    initialTypeLabel(descriptor),
                    targetFunction,
                    null,
                    null,
                    false,
                    false,
                    probeResult instanceof DispositionStatus.Drifted ? 0.5 : 0.0,
                    probeResult.getClass().getSimpleName());
        }

        final var evolutionResult = evolution.evaluate(descriptor, pending);

        if (evolutionResult instanceof EvolutionResult.Dampened dampened) {
            return new EvolutionTestResult(
                    initialTypeLabel(descriptor),
                    targetFunction,
                    pending.type().name(),
                    null,
                    false,
                    false,
                    0.0,
                    "Dampened(decay=" + dampened.decayFactor() + ")");
        }

        final var evolved = (EvolutionResult.Evolved) evolutionResult;
        final boolean structurallyValid = validateStructure(evolved.newProfile());
        final boolean weightTiersValid = validateWeightTiers(evolved.newProfile());
        final double psa = (structurallyValid && weightTiersValid) ? 1.0 : 0.0;

        return new EvolutionTestResult(
                evolved.previousTypeLabel(),
                targetFunction,
                pending.type().name(),
                evolved.newTypeLabel(),
                structurallyValid,
                weightTiersValid,
                psa,
                "Evolved");
    }

    private boolean validateStructure(final List<DispositionValue> profile) {
        if (profile == null || profile.size() < 2) return false;
        final var sorted = profile.stream()
                .sorted(Comparator.comparingDouble(DispositionValue::weight).reversed())
                .toList();
        final var dominant = sorted.get(0);
        final var auxiliary = sorted.get(1);
        return !dominant.term().equals(auxiliary.term())
                && dominant.weight() > auxiliary.weight();
    }

    private boolean validateWeightTiers(final List<DispositionValue> profile) {
        if (profile == null || profile.size() < 2) return false;
        final var sorted = profile.stream()
                .sorted(Comparator.comparingDouble(DispositionValue::weight).reversed())
                .toList();
        final double domWeight = sorted.get(0).weight();
        final double auxWeight = sorted.get(1).weight();
        return domWeight >= DOMINANT_MIN
                && auxWeight >= AUXILIARY_MIN
                && auxWeight <= AUXILIARY_MAX;
    }

    private String initialTypeLabel(final AgentDescriptor descriptor) {
        final var profile = descriptor.disposition().dispositionProfile();
        if (profile == null || profile.isEmpty()) return "unknown";
        final var sorted = profile.stream()
                .sorted(Comparator.comparingDouble(DispositionValue::weight).reversed())
                .toList();
        final var sb = new StringBuilder();
        sb.append(sorted.get(0).term().toUpperCase());
        if (sorted.size() > 1) {
            sb.append('-').append(sorted.get(1).term().toUpperCase());
        }
        return sb.toString();
    }

    public record EvolutionTestResult(
            String initialType,
            String targetFunction,
            String evolutionType,
            String resultingType,
            boolean structurallyValid,
            boolean weightTiersValid,
            double psa,
            String detail) {}
}

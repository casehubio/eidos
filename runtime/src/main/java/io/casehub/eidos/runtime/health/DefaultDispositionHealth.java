package io.casehub.eidos.runtime.health;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.CapabilityHealth.ProbeContext;
import io.casehub.eidos.api.DispositionHealth;
import io.casehub.eidos.api.DispositionSignalStore;
import io.casehub.eidos.api.DispositionValue;
import io.casehub.eidos.api.EvolutionType;
import io.casehub.eidos.api.VocabularyRegistry;
import io.casehub.eidos.api.VocabularyTerm;
import io.casehub.eidos.runtime.preferences.DispositionPreferenceKeys;
import io.casehub.platform.api.preferences.PreferenceProvider;
import io.casehub.platform.api.preferences.SettingsScope;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@ApplicationScoped
public class DefaultDispositionHealth implements DispositionHealth {

    static final EvolutionType DOMINANT_AUXILIARY_SWAP   = () -> "DOMINANT_AUXILIARY_SWAP";
    static final EvolutionType DOMINANT_REPLACEMENT      = () -> "DOMINANT_REPLACEMENT";
    static final EvolutionType AUXILIARY_REPLACEMENT     = () -> "AUXILIARY_REPLACEMENT";
    static final EvolutionType STRUCTURAL_REORGANIZATION = () -> "STRUCTURAL_REORGANIZATION";

    private final DispositionSignalStore       signalStore;
    private final VocabularyRegistry           vocabRegistry;
    private final Instance<PreferenceProvider> preferenceProviderInstance;

    @Inject
    public DefaultDispositionHealth(
            final DispositionSignalStore signalStore,
            final VocabularyRegistry vocabRegistry,
            final Instance<PreferenceProvider> preferenceProviderInstance) {
        this.signalStore                = signalStore;
        this.vocabRegistry              = vocabRegistry;
        this.preferenceProviderInstance = preferenceProviderInstance;
    }

    @Override
    public DispositionStatus probe(final AgentDescriptor descriptor,
                                   final ProbeContext context) {
        final var profile = descriptor.disposition().dispositionProfile();
        if (profile == null || profile.isEmpty()) {
            return new DispositionStatus.Aligned(Map.of());
        }

        final double delta = reinforcementDelta(descriptor.tenancyId());
        final var counts = signalStore.activationCounts(
                descriptor.agentId(), descriptor.tenancyId());

        final var base         = new LinkedHashMap<String, Double>();
        final var rawEffective = new LinkedHashMap<String, Double>();
        double    sum          = 0.0;

        for (final var dv : profile) {
            base.put(dv.term(), dv.weight());
            final double raw = dv.weight()
                               + counts.getOrDefault(dv.term(), 0) * delta;
            rawEffective.put(dv.term(), raw);
            sum += raw;
        }

        final var effective = new LinkedHashMap<String, Double>();
        for (final var entry : rawEffective.entrySet()) {
            effective.put(entry.getKey(), entry.getValue() / sum);
        }

        if (counts.isEmpty()) {
            return new DispositionStatus.Aligned(Map.copyOf(effective));
        }

        final var sorted = profile.stream()
                                  .sorted(Comparator.comparingDouble(DispositionValue::weight).reversed())
                                  .toList();
        final var dominant  = sorted.get(0);
        final var auxiliary = sorted.size() > 1 ? sorted.get(1) : null;

        // Over-reinforcement: dominant effective exceeds ceiling
        final double overThreshold = overReinforcementThreshold(descriptor.tenancyId());
        if (effective.getOrDefault(dominant.term(), 0.0) >= overThreshold) {
            final String mostActivated = counts.entrySet().stream()
                                               .max(Map.Entry.comparingByValue())
                                               .map(Map.Entry::getKey).orElse(dominant.term());
            return new DispositionStatus.Drifted(
                    Map.copyOf(effective), mostActivated, computeL2(base, effective));
        }

        if (auxiliary != null
            && effective.getOrDefault(auxiliary.term(), 0.0) >= dominant.weight()) {
            return new DispositionStatus.EvolutionPending(
                    DOMINANT_AUXILIARY_SWAP, auxiliary.term(), Map.copyOf(effective));
        }

        final String vocabUri = resolveVocabUri(descriptor);
        if (vocabUri != null && vocabRegistry.isRegistered(vocabUri)) {
            final var dominantShadow = findOpposite(vocabUri, dominant.term());
            final var auxiliaryShadow = auxiliary != null
                                        ? findOpposite(vocabUri, auxiliary.term()) : Optional.<String>empty();

            if (dominantShadow.isPresent()
                && effective.getOrDefault(dominantShadow.get(), 0.0) >= dominant.weight()) {
                return new DispositionStatus.EvolutionPending(
                        DOMINANT_REPLACEMENT, dominantShadow.get(), Map.copyOf(effective));
            }

            if (auxiliary != null && auxiliaryShadow.isPresent()
                && effective.getOrDefault(auxiliaryShadow.get(), 0.0) >= auxiliary.weight()) {
                return new DispositionStatus.EvolutionPending(
                        AUXILIARY_REPLACEMENT, auxiliaryShadow.get(), Map.copyOf(effective));
            }

            final Set<String> related = new HashSet<>();
            related.add(dominant.term());
            if (auxiliary != null) {related.add(auxiliary.term());}
            dominantShadow.ifPresent(related::add);
            auxiliaryShadow.ifPresent(related::add);

            for (final var entry : effective.entrySet()) {
                if (!related.contains(entry.getKey())
                    && entry.getValue() >= dominant.weight()) {
                    return new DispositionStatus.EvolutionPending(
                            STRUCTURAL_REORGANIZATION, entry.getKey(),
                            Map.copyOf(effective));
                }
            }
        }

        final String mostActivated = counts.entrySet().stream()
                                           .max(Map.Entry.comparingByValue())
                                           .map(Map.Entry::getKey)
                                           .orElse(null);

        final double driftMagnitude = computeL2(base, effective);

        if (mostActivated != null && driftMagnitude > 0.0) {
            return new DispositionStatus.Drifted(
                    Map.copyOf(effective), mostActivated, driftMagnitude);
        }

        return new DispositionStatus.Aligned(Map.copyOf(effective));
    }

    private double reinforcementDelta(final String tenancyId) {
        if (preferenceProviderInstance.isUnsatisfied()) {
            return DispositionPreferenceKeys.REINFORCEMENT_DELTA.defaultValue().value();
        }
        return preferenceProviderInstance.get()
                                         .resolve(SettingsScope.root(tenancyId))
                                         .getOrDefault(DispositionPreferenceKeys.REINFORCEMENT_DELTA).value();
    }

    private double overReinforcementThreshold(final String tenancyId) {
        if (preferenceProviderInstance.isUnsatisfied()) {
            return DispositionPreferenceKeys.OVER_REINFORCEMENT_THRESHOLD.defaultValue().value();
        }
        return preferenceProviderInstance.get()
                                         .resolve(SettingsScope.root(tenancyId))
                                         .getOrDefault(DispositionPreferenceKeys.OVER_REINFORCEMENT_THRESHOLD).value();
    }

    private String resolveVocabUri(final AgentDescriptor descriptor) {
        if (descriptor.dispositionVocabulary() != null
            && !descriptor.dispositionVocabulary().isBlank()) {
            return descriptor.dispositionVocabulary();
        }
        if (descriptor.domainVocabulary() != null
            && !descriptor.domainVocabulary().isBlank()) {
            return descriptor.domainVocabulary();
        }
        return null;
    }

    private Optional<String> findOpposite(final String vocabUri, final String term) {
        return vocabRegistry.resolve(vocabUri, term)
                            .flatMap(VocabularyTerm::opposite)
                            .map(VocabularyTerm::value);
    }

    private static double computeL2(final Map<String, Double> base,
                                    final Map<String, Double> effective) {
        double sumSq = 0.0;
        for (final var entry : base.entrySet()) {
            final double diff = effective.getOrDefault(entry.getKey(), 0.0) - entry.getValue();
            sumSq += diff * diff;
        }
        return Math.sqrt(sumSq);
    }
}

package io.casehub.eidos.runtime.health;

import io.casehub.eidos.api.DetectedRole;
import io.casehub.eidos.api.DetectedRoleQuery;
import io.casehub.eidos.api.DetectedRoleSignal;
import io.casehub.eidos.api.DetectedRoleSignalStore;
import io.casehub.eidos.api.TemporalDecay;
import io.casehub.eidos.core.preferences.StigmergyPreferenceKeys;
import io.casehub.platform.api.preferences.PreferenceProvider;
import io.casehub.platform.api.preferences.SettingsScope;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class DefaultDetectedRoleQuery implements DetectedRoleQuery {

    private final DetectedRoleSignalStore store;
    private final int windowDays;
    private final double halfLifeDays;
    private final Instance<PreferenceProvider> preferenceProviders;

    @Inject
    public DefaultDetectedRoleQuery(
            final DetectedRoleSignalStore store,
            @ConfigProperty(name = "casehub.eidos.stigmergy.window-days",
                            defaultValue = "90") final int windowDays,
            @ConfigProperty(name = "casehub.eidos.stigmergy.decay-half-life-days",
                            defaultValue = "90") final double halfLifeDays,
            final Instance<PreferenceProvider> preferenceProviders) {
        this.store = store;
        this.windowDays = windowDays;
        this.halfLifeDays = halfLifeDays;
        this.preferenceProviders = preferenceProviders;
    }

    @Override
    public List<DetectedRole> detectedRoles(final String agentId, final String tenancyId) {
        final Instant since = Instant.now().minus(windowDays, ChronoUnit.DAYS);
        final var allSignals = store.signalsByAgent(agentId, tenancyId, since);
        if (allSignals.isEmpty()) return List.of();

        final double threshold = confidenceThreshold(tenancyId);
        final int minCount = minSignalCount(tenancyId);
        final Instant now = Instant.now();

        final Map<String, List<DetectedRoleSignal>> byRole = allSignals.stream()
            .collect(Collectors.groupingBy(DetectedRoleSignal::roleName));

        return byRole.entrySet().stream()
            .map(e -> aggregate(e.getKey(), e.getValue(), now))
            .filter(r -> r.signalCount() >= minCount)
            .filter(r -> r.aggregateConfidence() >= threshold)
            .sorted(Comparator.comparingDouble(DetectedRole::aggregateConfidence).reversed())
            .toList();
    }

    private DetectedRole aggregate(final String roleName,
                                    final List<DetectedRoleSignal> signals,
                                    final Instant now) {
        double qualitySum = 0.0;
        double effectiveCount = 0.0;
        Instant first = Instant.MAX;
        Instant last = Instant.MIN;

        for (final var signal : signals) {
            final double decay = TemporalDecay.exponential(signal.observedAt(), now, halfLifeDays);
            qualitySum += signal.confidence() * decay;
            effectiveCount += decay;
            if (signal.observedAt().isBefore(first)) first = signal.observedAt();
            if (signal.observedAt().isAfter(last)) last = signal.observedAt();
        }

        final double aggConfidence = effectiveCount > 0.0 ? qualitySum / effectiveCount : 0.0;
        final var patternTypes = signals.stream()
            .map(DetectedRoleSignal::patternType)
            .distinct()
            .sorted()
            .toList();

        return new DetectedRole(roleName, aggConfidence, signals.size(),
                                 effectiveCount, first, last, patternTypes);
    }

    private double confidenceThreshold(final String tenancyId) {
        if (preferenceProviders.isUnsatisfied()) {
            return StigmergyPreferenceKeys.DETECTION_CONFIDENCE_THRESHOLD.defaultValue().value();
        }
        return preferenceProviders.get()
            .resolve(SettingsScope.root(tenancyId))
            .getOrDefault(StigmergyPreferenceKeys.DETECTION_CONFIDENCE_THRESHOLD).value();
    }

    private int minSignalCount(final String tenancyId) {
        if (preferenceProviders.isUnsatisfied()) {
            return StigmergyPreferenceKeys.MIN_SIGNAL_COUNT.defaultValue().value();
        }
        return preferenceProviders.get()
            .resolve(SettingsScope.root(tenancyId))
            .getOrDefault(StigmergyPreferenceKeys.MIN_SIGNAL_COUNT).value();
    }
}

package io.casehub.eidos.runtime.health;

import io.casehub.eidos.api.*;
import io.casehub.eidos.api.CapabilityHealth.CapabilityStatus.ExclusionSource;
import io.casehub.eidos.api.CapabilityResolver;
import io.casehub.eidos.runtime.preferences.EidosPreferenceKeys;
import io.casehub.platform.api.capacity.ActorCapacityView;
import io.casehub.platform.api.preferences.PreferenceProvider;
import io.casehub.platform.api.preferences.SettingsScope;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@DefaultBean
@ApplicationScoped
public class DefaultCapabilityHealth implements CapabilityHealth {

    private final double weakThreshold;
    private final double capacityThreshold;
    private final AgentStateStore stateStore;
    private final BehavioralSignalStore signalStore;
    private final Instance<PreferenceProvider> preferenceProviderInstance;
    private final Instance<ActorCapacityView> capacityViewInstance;
    private final VocabularyRegistry vocabularyRegistry;

    @Inject
    public DefaultCapabilityHealth(
            @ConfigProperty(name = "casehub.eidos.epistemic.weak-threshold", defaultValue = "0.3")
            final double weakThreshold,
            @ConfigProperty(name = "casehub.eidos.health.capacity-threshold", defaultValue = "0.8")
            final double capacityThreshold,
            final AgentStateStore stateStore,
            final BehavioralSignalStore signalStore,
            final Instance<PreferenceProvider> preferenceProviderInstance,
            final Instance<ActorCapacityView> capacityViewInstance,
            final VocabularyRegistry vocabularyRegistry) {
        this.weakThreshold = weakThreshold;
        this.capacityThreshold = capacityThreshold;
        this.stateStore = stateStore;
        this.signalStore = signalStore;
        this.preferenceProviderInstance = preferenceProviderInstance;
        this.capacityViewInstance = capacityViewInstance;
        this.vocabularyRegistry = vocabularyRegistry;
    }

    @Override
    public CapabilityStatus probe(final AgentDescriptor descriptor, final String capabilityTag,
                                  final ProbeContext context) {
        // Step 1: operational degradation takes priority
        final var degraded = stateStore.query(descriptor.agentId(), descriptor.tenancyId());
        if (degraded.isPresent()) {
            return new CapabilityStatus.Degraded(degraded.get(), "recorded at dispatch time");
        }

        // Step 2: capacity overload — live signal from ActorCapacityView
        if (capacityViewInstance.isResolvable()) {
            final var signal = capacityViewInstance.get()
                .aggregatedPressure(descriptor.agentId());
            if (signal != null && signal.pressure() >= capacityThreshold) {
                return new CapabilityStatus.Overloaded(signal.pressure(), capacityThreshold);
            }
        }

        // Step 3: capability not declared → unavailable
        if (descriptor.capabilities() == null || descriptor.capabilities().isEmpty()) {
            return new CapabilityStatus.Unavailable("Capability '" + capabilityTag + "' not declared");
        }

        final var resolved = CapabilityResolver.resolve(
            descriptor.capabilities(), capabilityTag, vocabularyRegistry);

        if (resolved == null) {
            return new CapabilityStatus.Unavailable("Capability '" + capabilityTag + "' not declared");
        }

        final var capability = resolved.capability();

        // Step 4: declared exclusion (null guard required — excludedDomains is nullable)
        if (context.taskDomain() != null
                && capability.excludedDomains() != null
                && capability.excludedDomains().contains(context.taskDomain())) {
            return new CapabilityStatus.Excluded(context.taskDomain(), ExclusionSource.DECLARED, 0);
        }

        // Step 5: learned exclusion — use declared capability name, not query tag
        if (context.taskDomain() != null) {
            final int count = signalStore.count(
                descriptor.agentId(), descriptor.tenancyId(), capability.name(),
                context.taskDomain(), BehavioralSignal.DECLINE);
            if (count >= excludeThreshold(descriptor.tenancyId())) {
                return new CapabilityStatus.Excluded(context.taskDomain(), ExclusionSource.LEARNED, count);
            }
        }

        // Step 6: epistemic weakness
        if (context.taskDomain() != null && capability.epistemicDomains() != null) {
            final Double confidence = capability.epistemicDomains().get(context.taskDomain());
            if (confidence != null && confidence < weakThreshold) {
                return new CapabilityStatus.EpistemicallyWeak(context.taskDomain(), confidence);
            }
        }

        // Step 7: behavioral compliance
        final var violations = signalStore.learned(
            descriptor.agentId(), descriptor.tenancyId(), capability.name(),
            BehavioralSignal.VIOLATED);
        if (!violations.isEmpty()) {
            final int threshold = complianceViolationThreshold(descriptor.tenancyId());
            final var exceeding = new LinkedHashMap<String, Integer>();
            violations.forEach((dimension, count) -> {
                if (count >= threshold) exceeding.put(dimension, count);
            });
            if (!exceeding.isEmpty()) {
                return new CapabilityStatus.BehavioralViolation(Map.copyOf(exceeding),
                    CapabilityStatus.BehavioralViolation.ViolationKind.PER_DIMENSION);
            }

            // Step 7b: aggregate check — total violations across all dimensions
            final int total = violations.values().stream().mapToInt(Integer::intValue).sum();
            if (total >= aggregateViolationThreshold(descriptor.tenancyId())) {
                return new CapabilityStatus.BehavioralViolation(Map.copyOf(violations),
                    CapabilityStatus.BehavioralViolation.ViolationKind.AGGREGATE);
            }
        }

        return new CapabilityStatus.Ready();
    }

    private int excludeThreshold(final String tenancyId) {
        if (preferenceProviderInstance.isUnsatisfied()) {
            return EidosPreferenceKeys.EXCLUDE_THRESHOLD.defaultValue().value();
        }
        return preferenceProviderInstance.get()
            .resolve(SettingsScope.root(tenancyId))
            .getOrDefault(EidosPreferenceKeys.EXCLUDE_THRESHOLD).value();
    }

    private int complianceViolationThreshold(final String tenancyId) {
        if (preferenceProviderInstance.isUnsatisfied()) {
            return EidosPreferenceKeys.COMPLIANCE_VIOLATION_THRESHOLD.defaultValue().value();
        }
        return preferenceProviderInstance.get()
            .resolve(SettingsScope.root(tenancyId))
            .getOrDefault(EidosPreferenceKeys.COMPLIANCE_VIOLATION_THRESHOLD).value();
    }

    private int aggregateViolationThreshold(final String tenancyId) {
        if (preferenceProviderInstance.isUnsatisfied()) {
            return EidosPreferenceKeys.AGGREGATE_VIOLATION_THRESHOLD.defaultValue().value();
        }
        return preferenceProviderInstance.get()
            .resolve(SettingsScope.root(tenancyId))
            .getOrDefault(EidosPreferenceKeys.AGGREGATE_VIOLATION_THRESHOLD).value();
    }
}

package io.casehub.eidos.runtime.health;

import io.casehub.eidos.api.*;
import io.casehub.eidos.api.CapabilityHealth.CapabilityStatus.ExclusionSource;
import io.casehub.eidos.api.SpecializationSignal;
import io.casehub.eidos.runtime.preferences.EidosPreferenceKeys;
import io.casehub.platform.api.preferences.PreferenceProvider;
import io.casehub.platform.api.preferences.SettingsScope;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@DefaultBean
@ApplicationScoped
public class DefaultCapabilityHealth implements CapabilityHealth {

    private final double weakThreshold;
    private final AgentStateStore stateStore;
    private final CapabilitySpecializationStore specializationStore;
    private final Instance<PreferenceProvider> preferenceProviderInstance;

    @Inject
    public DefaultCapabilityHealth(
            @ConfigProperty(name = "casehub.eidos.epistemic.weak-threshold", defaultValue = "0.3")
            final double weakThreshold,
            final AgentStateStore stateStore,
            final CapabilitySpecializationStore specializationStore,
            final Instance<PreferenceProvider> preferenceProviderInstance) {
        this.weakThreshold = weakThreshold;
        this.stateStore = stateStore;
        this.specializationStore = specializationStore;
        this.preferenceProviderInstance = preferenceProviderInstance;
    }

    @Override
    public CapabilityStatus probe(final AgentDescriptor descriptor, final String capabilityTag,
                                  final ProbeContext context) {
        // Step 1: operational degradation takes priority
        final var degraded = stateStore.query(descriptor.agentId(), descriptor.tenancyId());
        if (degraded.isPresent()) {
            return new CapabilityStatus.Degraded(degraded.get(), "recorded at dispatch time");
        }

        // Step 2: capability not declared → unavailable
        if (descriptor.capabilities() == null || descriptor.capabilities().isEmpty()) {
            return new CapabilityStatus.Unavailable("Capability '" + capabilityTag + "' not declared");
        }

        final var capability = descriptor.capabilities().stream()
                .filter(c -> c.name().equals(capabilityTag))
                .findFirst()
                .orElse(null);

        if (capability == null) {
            return new CapabilityStatus.Unavailable("Capability '" + capabilityTag + "' not declared");
        }

        // Step 3: declared exclusion (null guard required — excludedDomains is nullable)
        if (context.taskDomain() != null
                && capability.excludedDomains() != null
                && capability.excludedDomains().contains(context.taskDomain())) {
            return new CapabilityStatus.Excluded(context.taskDomain(), ExclusionSource.DECLARED, 0);
        }

        // Step 4: learned exclusion — single count call (count used for threshold check and Excluded record)
        if (context.taskDomain() != null) {
            final int count = specializationStore.count(
                descriptor.agentId(), descriptor.tenancyId(), capabilityTag,
                context.taskDomain(), SpecializationSignal.DECLINE);
            if (count >= excludeThreshold(descriptor.tenancyId())) {
                return new CapabilityStatus.Excluded(context.taskDomain(), ExclusionSource.LEARNED, count);
            }
        }

        // Step 5: epistemic weakness
        if (context.taskDomain() != null && capability.epistemicDomains() != null) {
            final Double confidence = capability.epistemicDomains().get(context.taskDomain());
            if (confidence != null && confidence < weakThreshold) {
                return new CapabilityStatus.EpistemicallyWeak(context.taskDomain(), confidence);
            }
        }

        return new CapabilityStatus.Ready();
    }

    private int excludeThreshold(final String tenancyId) {
        if (preferenceProviderInstance.isUnsatisfied()) {
            return EidosPreferenceKeys.EXCLUDE_THRESHOLD.defaultValue().value();
        }
        return preferenceProviderInstance.get()
            .resolve(SettingsScope.of(tenancyId))
            .getOrDefault(EidosPreferenceKeys.EXCLUDE_THRESHOLD).value();
    }
}

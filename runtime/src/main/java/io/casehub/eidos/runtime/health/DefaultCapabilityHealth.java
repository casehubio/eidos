package io.casehub.eidos.runtime.health;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentStateStore;
import io.casehub.eidos.api.CapabilityHealth;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class DefaultCapabilityHealth implements CapabilityHealth {

    private final double weakThreshold;
    private final AgentStateStore stateStore;

    @Inject
    public DefaultCapabilityHealth(
            @ConfigProperty(name = "casehub.eidos.epistemic.weak-threshold", defaultValue = "0.3")
            final double weakThreshold,
            final AgentStateStore stateStore) {
        this.weakThreshold = weakThreshold;
        this.stateStore = stateStore;
    }

    @Override
    public CapabilityStatus probe(final AgentDescriptor descriptor, final String capabilityTag,
                                  final ProbeContext context) {
        final var degraded = stateStore.query(descriptor.agentId(), descriptor.tenancyId());
        if (degraded.isPresent()) {
            return new CapabilityStatus.Degraded(degraded.get(), "recorded at dispatch time");
        }

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

        if (context.taskDomain() != null && capability.epistemicDomains() != null) {
            final Double confidence = capability.epistemicDomains().get(context.taskDomain());
            if (confidence != null && confidence < weakThreshold) {
                return new CapabilityStatus.EpistemicallyWeak(context.taskDomain(), confidence);
            }
        }

        return new CapabilityStatus.Ready();
    }
}

package io.casehub.eidos.runtime.health;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.CapabilityHealth;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class DefaultCapabilityHealth implements CapabilityHealth {

    @ConfigProperty(name = "casehub.eidos.epistemic.weak-threshold", defaultValue = "0.3")
    double weakThreshold;

    @Override
    public CapabilityStatus probe(AgentDescriptor descriptor, String capabilityTag, ProbeContext context) {
        if (descriptor.capabilities() == null || descriptor.capabilities().isEmpty()) {
            return new CapabilityStatus.Unavailable(
                "Capability '" + capabilityTag + "' not declared");
        }

        var capability = descriptor.capabilities().stream()
            .filter(c -> c.name().equals(capabilityTag))
            .findFirst()
            .orElse(null);

        if (capability == null) {
            return new CapabilityStatus.Unavailable(
                "Capability '" + capabilityTag + "' not declared");
        }

        if (context.taskDomain() != null && capability.epistemicDomains() != null) {
            Double confidence = capability.epistemicDomains().get(context.taskDomain());
            if (confidence != null && confidence < weakThreshold) {
                return new CapabilityStatus.EpistemicallyWeak(context.taskDomain(), confidence);
            }
        }

        return new CapabilityStatus.Ready();
    }
}

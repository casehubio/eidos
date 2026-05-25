package io.casehub.eidos.api;

import java.util.Map;

public interface CapabilityHealth {
    CapabilityStatus probe(AgentDescriptor descriptor, String capabilityTag, ProbeContext context);

    record ProbeContext(String taskDomain, Map<String, Object> taskMetadata) {
        public static ProbeContext of(String taskDomain) {
            return new ProbeContext(taskDomain, Map.of());
        }
    }

    sealed interface CapabilityStatus permits
            CapabilityStatus.Ready,
            CapabilityStatus.Degraded,
            CapabilityStatus.Unavailable,
            CapabilityStatus.EpistemicallyWeak {

        record Ready() implements CapabilityStatus {}
        record Degraded(DegradationReason reason, String detail) implements CapabilityStatus {}
        record Unavailable(String reason) implements CapabilityStatus {}
        record EpistemicallyWeak(String domain, double confidence) implements CapabilityStatus {}
    }
}

package io.casehub.eidos.org.api;

import io.casehub.eidos.api.BehavioralSignal;

import java.util.Set;

public record AttestationGrant(
    Set<String> dimensions,
    Set<String> capabilityScope,
    Set<BehavioralSignal> signalTypes
) {
    public AttestationGrant {
        if (dimensions == null || dimensions.isEmpty()) {
            throw new IllegalArgumentException("AttestationGrant requires at least one dimension");
        }
        dimensions = Set.copyOf(dimensions);
        capabilityScope = capabilityScope != null ? Set.copyOf(capabilityScope) : Set.of();
        signalTypes = signalTypes != null ? Set.copyOf(signalTypes) : Set.of();
    }
}

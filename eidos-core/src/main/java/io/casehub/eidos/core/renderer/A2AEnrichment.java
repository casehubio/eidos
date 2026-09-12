package io.casehub.eidos.core.renderer;

import java.util.List;

public record A2AEnrichment(List<CapabilityNarrative> capabilityNarratives) {
    record CapabilityNarrative(String name, String description) {}
}

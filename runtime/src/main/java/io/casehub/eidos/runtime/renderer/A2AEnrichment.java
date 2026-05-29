package io.casehub.eidos.runtime.renderer;

import java.util.List;

record A2AEnrichment(List<CapabilityNarrative> capabilityNarratives) {
    record CapabilityNarrative(String name, String description) {}
}

package io.casehub.eidos.runtime.renderer;

import java.util.Optional;

record SemanticEnrichment(
        String identityNarrative,
        String roleNarrative,
        String capabilityNarrative,
        Optional<String> dispositionNarrative,
        Optional<String> constraintNarrative,
        Optional<String> goalNarrative
) {}

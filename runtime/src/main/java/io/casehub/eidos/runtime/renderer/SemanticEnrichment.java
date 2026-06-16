package io.casehub.eidos.runtime.renderer;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;

record SemanticEnrichment(
        Optional<String> dispositionNarrative,
        Optional<String> goalNarrative
) {
    static Optional<String> parseOptional(final JsonNode node, final String field) {
        final JsonNode n = node.get(field);
        if (n == null || n.isNull()) return Optional.empty();
        final String v = n.asText("").strip();
        return v.isEmpty() ? Optional.empty() : Optional.of(v);
    }
}

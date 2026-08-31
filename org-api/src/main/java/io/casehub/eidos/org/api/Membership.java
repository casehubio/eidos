package io.casehub.eidos.org.api;

import java.util.Objects;

public record Membership(
    String agentId,
    String role,
    String roleVocabulary
) {
    public Membership {
        Objects.requireNonNull(agentId, "agentId");
    }
}

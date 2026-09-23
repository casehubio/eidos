package io.casehub.eidos.api;

import java.util.Objects;
import java.util.Set;

public record Collaborator(
    String agentId,
    String tenancyId,
    Set<CollaborationRelation> relations,
    double affinity
) {
    public Collaborator {
        Objects.requireNonNull(agentId, "agentId");
        Objects.requireNonNull(tenancyId, "tenancyId");
        relations = Set.copyOf(relations);
        if (affinity < 0.0 || affinity > 1.0) {
            throw new IllegalArgumentException("affinity must be in [0.0, 1.0]");
        }
    }
}

package io.casehub.eidos.org.api;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record FormationProposal(
    String specId,
    String tenancyId,
    FormationOrigin origin,
    List<String> proposedMembers,
    Map<String, String> metadata
) {
    public FormationProposal {
        Objects.requireNonNull(specId, "specId");
        Objects.requireNonNull(tenancyId, "tenancyId");
        Objects.requireNonNull(origin, "origin");
        proposedMembers = proposedMembers != null ? List.copyOf(proposedMembers) : List.of();
        metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    }
}

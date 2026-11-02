package io.casehub.eidos.org.api;

import java.util.Objects;

public record GroupInstanceQuery(
    String tenancyId,
    String specId,
    GroupLifecycleState state,
    String agentId
) {
    public GroupInstanceQuery {
        Objects.requireNonNull(tenancyId, "tenancyId");
    }
}

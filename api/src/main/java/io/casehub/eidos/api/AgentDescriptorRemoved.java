package io.casehub.eidos.api;

import java.util.Objects;

public record AgentDescriptorRemoved(String agentId, String tenancyId) {
    public AgentDescriptorRemoved {
        Objects.requireNonNull(agentId, "agentId");
        Objects.requireNonNull(tenancyId, "tenancyId");
    }
}

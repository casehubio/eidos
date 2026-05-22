package io.casehub.eidos.api;

import java.util.Objects;

public record AgentQuery(
        String slot,
        String capabilityName,
        String tenancyId
) {
    public AgentQuery {
        Objects.requireNonNull(tenancyId, "tenancyId must not be null");
    }

    public static AgentQuery bySlot(String slot, String tenancyId) {
        return new AgentQuery(slot, null, tenancyId);
    }

    public static AgentQuery byCapability(String capabilityName, String tenancyId) {
        return new AgentQuery(null, capabilityName, tenancyId);
    }

    public static AgentQuery bySlotAndCapability(String slot, String capabilityName, String tenancyId) {
        return new AgentQuery(slot, capabilityName, tenancyId);
    }

    public static AgentQuery all(String tenancyId) {
        return new AgentQuery(null, null, tenancyId);
    }
}

package io.casehub.eidos.api;

import java.util.Objects;

public record AgentQuery(
        String slot,
        String capabilityName,
        String tenancyId,
        String taskDomain
) {
    /**
     * @throws NullPointerException if tenancyId is null
     */
    public AgentQuery {
        Objects.requireNonNull(tenancyId, "tenancyId");
    }

    public static AgentQuery bySlot(String slot, String tenancyId) {
        return new AgentQuery(slot, null, tenancyId, null);
    }

    public static AgentQuery byCapability(String capabilityName, String tenancyId) {
        return new AgentQuery(null, capabilityName, tenancyId, null);
    }

    public static AgentQuery bySlotAndCapability(String slot, String capabilityName, String tenancyId) {
        return new AgentQuery(slot, capabilityName, tenancyId, null);
    }

    public static AgentQuery byCapabilityAndDomain(String capabilityName, String taskDomain, String tenancyId) {
        return new AgentQuery(null, capabilityName, tenancyId, taskDomain);
    }

    public static AgentQuery all(String tenancyId) {
        return new AgentQuery(null, null, tenancyId, null);
    }
}

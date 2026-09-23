package io.casehub.eidos.api;

import java.util.Objects;

public record AgentQuery(
        String slot,
        String capabilityName,
        String tenancyId,
        String taskDomain,
        String goalName,
        Integer maxDepth
) {
    public AgentQuery {
        Objects.requireNonNull(tenancyId, "tenancyId");
        if (maxDepth != null && maxDepth < 1) {
            throw new IllegalArgumentException("maxDepth must be >= 1");
        }
    }

    public static AgentQuery bySlot(String slot, String tenancyId) {
        return new AgentQuery(slot, null, tenancyId, null, null, null);
    }

    public static AgentQuery byCapability(String capabilityName, String tenancyId) {
        return new AgentQuery(null, capabilityName, tenancyId, null, null, null);
    }

    public static AgentQuery bySlotAndCapability(String slot, String capabilityName, String tenancyId) {
        return new AgentQuery(slot, capabilityName, tenancyId, null, null, null);
    }

    public static AgentQuery byCapabilityAndDomain(String capabilityName, String taskDomain, String tenancyId) {
        return new AgentQuery(null, capabilityName, tenancyId, taskDomain, null, null);
    }

    public static AgentQuery byGoal(String goalName, String tenancyId) {
        return new AgentQuery(null, null, tenancyId, null, goalName, null);
    }

    public static AgentQuery all(String tenancyId) {
        return new AgentQuery(null, null, tenancyId, null, null, null);
    }

    public static AgentQuery byProximity(String capabilityName, int maxDepth, String tenancyId) {
        return new AgentQuery(null, capabilityName, tenancyId, null, null, maxDepth);
    }

    public static AgentQuery byProximityAndDomain(
            String capabilityName, int maxDepth, String taskDomain, String tenancyId) {
        return new AgentQuery(null, capabilityName, tenancyId, taskDomain, null, maxDepth);
    }
}

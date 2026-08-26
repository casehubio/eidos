package io.casehub.eidos.api;

public sealed interface AgentSelection {

    /**
     * @param resolvedCapability the capability resolution result, or null when no capability was queried
     */
    record Selected(
        AgentDescriptor agent,
        ResolvedCapability resolvedCapability,
        double trustScore,
        String reason
    ) implements AgentSelection {}

    record NoneQualified(String reason) implements AgentSelection {}

    record Escalated(
        String capabilityName,
        EscalationKind kind,
        String reason
    ) implements AgentSelection {}
}

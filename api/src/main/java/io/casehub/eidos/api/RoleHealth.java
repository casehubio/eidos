package io.casehub.eidos.api;

import io.casehub.eidos.api.CapabilityHealth.ProbeContext;

public interface RoleHealth {

    RoleStatus probe(AgentDescriptor descriptor, String roleName, ProbeContext context);

    default java.util.List<RoleEvidence> lastProbeEvidence() {
        return java.util.List.of();
    }


    sealed interface RoleStatus permits
            RoleStatus.DeclaredFit,
            RoleStatus.Fit,
            RoleStatus.Marginal,
            RoleStatus.Unfit,
            RoleStatus.Undeclared {

        String roleName();

        record DeclaredFit(String roleName) implements RoleStatus {}

        record Fit(String roleName, double confidence) implements RoleStatus {}

        record Marginal(String roleName, double confidence, int totalOutcomes)
                implements RoleStatus {}

        record Unfit(String roleName, double confidence, int failureCount,
                     int totalOutcomes) implements RoleStatus {}

        record Undeclared(String roleName) implements RoleStatus {}
    }
}

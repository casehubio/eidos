package io.casehub.eidos.api;

import io.casehub.eidos.api.CapabilityHealth.ProbeContext;

public interface RoleEvidenceSource {
    RoleEvidence evaluate(String agentId, String roleName,
                          String tenancyId, ProbeContext context);
    String sourceId();
}

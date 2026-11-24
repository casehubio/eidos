package io.casehub.eidos.core.health;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.CapabilityHealth.ProbeContext;
import io.casehub.eidos.api.RoleHealth;

public class NoOpRoleHealth implements RoleHealth {
    @Override
    public RoleStatus probe(final AgentDescriptor descriptor, final String roleName,
                            final ProbeContext context) {
        return new RoleStatus.DeclaredFit(roleName);
    }
}

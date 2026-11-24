package io.casehub.eidos.core.health;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.RoleHealth.RoleStatus;
import io.casehub.eidos.api.RoleTransitionAdvisor;

public class NoOpRoleTransitionAdvisor implements RoleTransitionAdvisor {
    @Override
    public TransitionAdvice evaluate(final AgentDescriptor descriptor,
                                     final RoleStatus currentStatus) {
        return new TransitionAdvice.PolicyDisabled();
    }
}

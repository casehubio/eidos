package io.casehub.eidos.core.health;

import io.casehub.eidos.api.RoleHoldCycleStore;

public class NoOpRoleHoldCycleStore implements RoleHoldCycleStore {
    @Override
    public HoldCycleState get(final String agentId, final String roleName,
                              final String tenancyId) {
        return HoldCycleState.EMPTY;
    }

    @Override
    public void increment(final String agentId, final String roleName,
                          final String tenancyId) {}

    @Override
    public void reset(final String agentId, final String roleName,
                      final String tenancyId) {}
}

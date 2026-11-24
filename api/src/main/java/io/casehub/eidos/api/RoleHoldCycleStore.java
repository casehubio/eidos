package io.casehub.eidos.api;

import java.time.Instant;

public interface RoleHoldCycleStore {

    HoldCycleState get(String agentId, String roleName, String tenancyId);

    void increment(String agentId, String roleName, String tenancyId);

    void reset(String agentId, String roleName, String tenancyId);

    record HoldCycleState(int consecutiveCycles, Instant lastEvaluatedAt) {
        public static final HoldCycleState EMPTY = new HoldCycleState(0, null);
    }
}

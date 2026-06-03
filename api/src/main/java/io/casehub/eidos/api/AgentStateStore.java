package io.casehub.eidos.api;

import java.time.Instant;
import java.util.Optional;

public interface AgentStateStore {
    void record(String agentId, String tenancyId, DegradationReason reason, Instant expiresAt);
    Optional<DegradationReason> query(String agentId, String tenancyId);
    void clear(String agentId, String tenancyId);
}

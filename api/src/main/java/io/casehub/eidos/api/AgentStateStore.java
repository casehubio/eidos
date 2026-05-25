package io.casehub.eidos.api;

import java.time.Instant;
import java.util.Optional;

public interface AgentStateStore {
    void record(String agentId, DegradationReason reason, Instant expiresAt);
    Optional<DegradationReason> query(String agentId);
    void clear(String agentId);
}

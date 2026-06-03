package io.casehub.eidos.api;

import io.smallrye.mutiny.Uni;
import java.time.Instant;
import java.util.Optional;

public interface ReactiveAgentStateStore {
    Uni<Void>                        record(String agentId, String tenancyId, DegradationReason reason, Instant expiresAt);
    Uni<Optional<DegradationReason>> query(String agentId, String tenancyId);
    Uni<Void>                        clear(String agentId, String tenancyId);
}

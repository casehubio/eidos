package io.casehub.eidos.api;

import io.smallrye.mutiny.Uni;
import java.time.Instant;
import java.util.Optional;

public interface ReactiveAgentStateStore {
    Uni<Void>                        record(String agentId, DegradationReason reason, Instant expiresAt);
    Uni<Optional<DegradationReason>> query(String agentId);
    Uni<Void>                        clear(String agentId);
}

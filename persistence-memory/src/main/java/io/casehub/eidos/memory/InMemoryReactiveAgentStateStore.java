package io.casehub.eidos.memory;

import io.casehub.eidos.api.DegradationReason;
import io.casehub.eidos.api.ReactiveAgentStateStore;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.Optional;

@Alternative
@Priority(1)
@ApplicationScoped
public class InMemoryReactiveAgentStateStore implements ReactiveAgentStateStore {

    @Inject InMemoryAgentStateStore delegate;

    // Package-private constructor for pure-Java tests (no CDI)
    InMemoryReactiveAgentStateStore(final InMemoryAgentStateStore delegate) {
        this.delegate = delegate;
    }

    // CDI no-arg constructor (Weld requires it when a non-default constructor exists)
    InMemoryReactiveAgentStateStore() {}

    @Override
    public Uni<Void> record(final String agentId, final String tenancyId,
                            final DegradationReason reason, final Instant expiresAt) {
        return Uni.createFrom().<Void>item(() -> {
            delegate.record(agentId, tenancyId, reason, expiresAt);
            return null;
        });
    }

    @Override
    public Uni<Optional<DegradationReason>> query(final String agentId, final String tenancyId) {
        return Uni.createFrom().item(() -> delegate.query(agentId, tenancyId));
    }

    @Override
    public Uni<Void> clear(final String agentId, final String tenancyId) {
        return Uni.createFrom().<Void>item(() -> {
            delegate.clear(agentId, tenancyId);
            return null;
        });
    }
}

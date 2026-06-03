package io.casehub.eidos.runtime.health;

import io.casehub.eidos.api.AgentStateStore;
import io.casehub.eidos.api.DegradationReason;
import io.casehub.eidos.api.ReactiveAgentStateStore;
import io.quarkus.arc.DefaultBean;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.Optional;

@DefaultBean
@ApplicationScoped
public class DefaultReactiveAgentStateStore implements ReactiveAgentStateStore {

    @Inject
    AgentStateStore delegate;

    @Override
    public Uni<Void> record(final String agentId, final String tenancyId,
                            final DegradationReason reason, final Instant expiresAt) {
        return Uni.createFrom().<Void>item(() -> {
            delegate.record(agentId, tenancyId, reason, expiresAt);
            return null;
        }).runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    @Override
    public Uni<Optional<DegradationReason>> query(final String agentId, final String tenancyId) {
        return Uni.createFrom()
                  .item(() -> delegate.query(agentId, tenancyId))
                  .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    @Override
    public Uni<Void> clear(final String agentId, final String tenancyId) {
        return Uni.createFrom().<Void>item(() -> {
            delegate.clear(agentId, tenancyId);
            return null;
        }).runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }
}

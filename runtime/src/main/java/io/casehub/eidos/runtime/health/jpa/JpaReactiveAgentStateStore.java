package io.casehub.eidos.runtime.health.jpa;

import io.casehub.eidos.api.DegradationReason;
import io.casehub.eidos.api.ReactiveAgentStateStore;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.Optional;

@IfBuildProperty(name = "casehub.eidos.reactive.enabled", stringValue = "true")
@ApplicationScoped
public class JpaReactiveAgentStateStore implements ReactiveAgentStateStore {

    @Inject
    AgentDegradationStateReactivePanacheRepo repo;

    @Override
    @WithTransaction
    public Uni<Void> record(final String agentId, final DegradationReason reason, final Instant expiresAt) {
        return repo.delete("agentId", agentId)
                   .chain(() -> repo.persist(new AgentDegradationStateEntity(agentId, reason.name(), expiresAt)))
                   .replaceWithVoid();
    }

    @Override
    @WithSession
    public Uni<Optional<DegradationReason>> query(final String agentId) {
        return repo.find("agentId = ?1 AND expiresAt > ?2", agentId, Instant.now())
                   .firstResult()
                   .map(e -> Optional.ofNullable(e)
                                     .map(entity -> DegradationReason.valueOf(entity.getDegradationReason())));
    }

    @Override
    @WithTransaction
    public Uni<Void> clear(final String agentId) {
        return repo.delete("agentId", agentId).replaceWithVoid();
    }
}

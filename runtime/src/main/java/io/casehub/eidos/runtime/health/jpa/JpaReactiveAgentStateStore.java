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
    AgentStateReactivePanacheRepo repo;

    @Override
    @WithTransaction
    public Uni<Void> record(final String agentId, final String tenancyId,
                            final DegradationReason reason, final Instant expiresAt) {
        return repo.delete("id.agentId = ?1 AND id.tenancyId = ?2", agentId, tenancyId)
                   .chain(() -> repo.persist(new AgentStateEntity(agentId, tenancyId, reason.name(), expiresAt)))
                   .replaceWithVoid();
    }

    @Override
    @WithSession
    public Uni<Optional<DegradationReason>> query(final String agentId, final String tenancyId) {
        return repo.find("id.agentId = ?1 AND id.tenancyId = ?2 AND expiresAt > ?3",
                         agentId, tenancyId, Instant.now())
                   .firstResult()
                   .map(e -> Optional.ofNullable(e)
                                     .map(entity -> DegradationReason.valueOf(entity.getDegradation())));
    }

    @Override
    @WithTransaction
    public Uni<Void> clear(final String agentId, final String tenancyId) {
        return repo.delete("id.agentId = ?1 AND id.tenancyId = ?2", agentId, tenancyId)
                   .replaceWithVoid();
    }
}

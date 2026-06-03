package io.casehub.eidos.runtime.health.jpa;

import io.casehub.eidos.api.AgentStateStore;
import io.casehub.eidos.api.DegradationReason;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import java.time.Instant;
import java.util.Optional;

@IfBuildProperty(name = "casehub.eidos.reactive.enabled", stringValue = "false", enableIfMissing = true)
@ApplicationScoped
public class JpaAgentStateStore implements AgentStateStore {

    @Inject EntityManager em;

    @Override
    @Transactional
    public void record(final String agentId, final String tenancyId,
                       final DegradationReason reason, final Instant expiresAt) {
        em.createQuery("DELETE FROM AgentStateEntity e WHERE e.id.agentId = :id AND e.id.tenancyId = :tenancyId")
          .setParameter("id", agentId)
          .setParameter("tenancyId", tenancyId)
          .executeUpdate();
        // flush + clear: without these, Hibernate's first-level cache retains the deleted entity
        // and throws EntityExistsException on the subsequent persist
        em.flush();
        em.clear();
        em.persist(new AgentStateEntity(agentId, tenancyId, reason.name(), expiresAt));
    }

    @Override
    @Transactional(TxType.SUPPORTS)
    public Optional<DegradationReason> query(final String agentId, final String tenancyId) {
        return em.createQuery(
                "SELECT e FROM AgentStateEntity e WHERE e.id.agentId = :id"
                + " AND e.id.tenancyId = :tenancyId AND e.expiresAt > :now",
                AgentStateEntity.class)
            .setParameter("id", agentId)
            .setParameter("tenancyId", tenancyId)
            .setParameter("now", Instant.now())
            .getResultStream()
            .findFirst()
            .map(e -> DegradationReason.valueOf(e.getDegradation()));
    }

    @Override
    @Transactional
    public void clear(final String agentId, final String tenancyId) {
        em.createQuery("DELETE FROM AgentStateEntity e WHERE e.id.agentId = :id AND e.id.tenancyId = :tenancyId")
          .setParameter("id", agentId)
          .setParameter("tenancyId", tenancyId)
          .executeUpdate();
    }
}

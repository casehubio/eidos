package io.casehub.eidos.runtime.health.jpa;

import io.casehub.eidos.api.AgentStateStore;
import io.casehub.eidos.api.DegradationReason;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.annotation.PostConstruct;
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
    private boolean       postgres;

    @PostConstruct
    void init() {
        postgres = JpaDialect.isPostgres(em);
    }

    @Override
    @Transactional
    public void record(final String agentId, final String tenancyId,
                       final DegradationReason reason, final Instant expiresAt) {
        if (postgres) {
            em.createNativeQuery(
                      "INSERT INTO agent_state (agent_id, tenancy_id, degradation, expires_at)"
                      + " VALUES (:agentId, :tenancyId, :degradation, :expiresAt)"
                      + " ON CONFLICT (agent_id, tenancy_id)"
                      + " DO UPDATE SET degradation = EXCLUDED.degradation,"
                      + " expires_at = EXCLUDED.expires_at")
              .setParameter("agentId", agentId)
              .setParameter("tenancyId", tenancyId)
              .setParameter("degradation", reason.name())
              .setParameter("expiresAt", expiresAt)
              .executeUpdate();
        } else {
            em.createNativeQuery(
                      "MERGE INTO agent_state AS s"
                      + " USING (VALUES (CAST(:agentId AS VARCHAR), CAST(:tenancyId AS VARCHAR),"
                      + " CAST(:degradation AS VARCHAR),"
                      + " CAST(:expiresAt AS TIMESTAMP WITH TIME ZONE)))"
                      + " AS src(agent_id, tenancy_id, degradation, expires_at)"
                      + " ON s.agent_id = src.agent_id"
                      + " AND s.tenancy_id = src.tenancy_id"
                      + " WHEN MATCHED THEN UPDATE SET degradation = src.degradation,"
                      + " expires_at = src.expires_at"
                      + " WHEN NOT MATCHED THEN INSERT (agent_id, tenancy_id, degradation, expires_at)"
                      + " VALUES (src.agent_id, src.tenancy_id, src.degradation, src.expires_at)")
              .setParameter("agentId", agentId)
              .setParameter("tenancyId", tenancyId)
              .setParameter("degradation", reason.name())
              .setParameter("expiresAt", expiresAt)
              .executeUpdate();
        }
        em.flush();
        em.clear();
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

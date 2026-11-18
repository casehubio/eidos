package io.casehub.eidos.runtime.health.jpa;

import io.casehub.eidos.api.DispositionSignalStore;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import jakarta.annotation.PostConstruct;

import java.util.HashMap;
import java.util.Map;

@IfBuildProperty(name = "casehub.eidos.reactive.enabled", stringValue = "false", enableIfMissing = true)
@ApplicationScoped
public class JpaDispositionSignalStore implements DispositionSignalStore {

    @Inject EntityManager em;
    private boolean       postgres;

    @PostConstruct
    void init() {
        postgres = JpaDialect.isPostgres(em);
    }

    @Override
    @Transactional(TxType.REQUIRES_NEW)
    public void recordActivation(final String agentId, final String tenancyId,
                                 final String functionTerm) {
        if (postgres) {
            em.createNativeQuery(
                      "INSERT INTO disposition_signal (agent_id, tenancy_id, function_term, weight)"
                      + " VALUES (:agentId, :tenancyId, :functionTerm, 1.0)"
                      + " ON CONFLICT (agent_id, tenancy_id, function_term)"
                      + " DO UPDATE SET weight = disposition_signal.weight + 1")
              .setParameter("agentId", agentId)
              .setParameter("tenancyId", tenancyId)
              .setParameter("functionTerm", functionTerm)
              .executeUpdate();
        } else {
            em.createNativeQuery(
                      "MERGE INTO disposition_signal AS ds"
                      + " USING (VALUES (CAST(:agentId AS VARCHAR), CAST(:tenancyId AS VARCHAR),"
                      + " CAST(:functionTerm AS VARCHAR)))"
                      + " AS src(agent_id, tenancy_id, function_term)"
                      + " ON ds.agent_id = src.agent_id"
                      + " AND ds.tenancy_id = src.tenancy_id"
                      + " AND ds.function_term = src.function_term"
                      + " WHEN MATCHED THEN UPDATE SET weight = ds.weight + 1"
                      + " WHEN NOT MATCHED THEN INSERT (agent_id, tenancy_id, function_term, weight)"
                      + " VALUES (src.agent_id, src.tenancy_id, src.function_term, 1.0)")
              .setParameter("agentId", agentId)
              .setParameter("tenancyId", tenancyId)
              .setParameter("functionTerm", functionTerm)
              .executeUpdate();
        }
    }

    @Override
    @Transactional(TxType.SUPPORTS)
    public Map<String, Double> activationWeights(final String agentId,
                                                 final String tenancyId) {
        final var results = em.createQuery(
                                      "SELECT e FROM DispositionSignalEntity e"
                                      + " WHERE e.id.agentId = :agentId"
                                      + " AND e.id.tenancyId = :tenancyId"
                                      + " AND e.weight > 0",
                                      DispositionSignalEntity.class)
                              .setParameter("agentId", agentId)
                              .setParameter("tenancyId", tenancyId)
                              .getResultList();

        final var map = new HashMap<String, Double>();
        for (final var e : results) {
            map.put(e.id.functionTerm, e.weight);
        }
        return Map.copyOf(map);
    }

    @Override
    @Transactional
    public void decay(final String agentId, final String tenancyId,
                      final double decayFactor) {
        em.createQuery(
                  "UPDATE DispositionSignalEntity e SET e.weight = e.weight * :factor"
                  + " WHERE e.id.agentId = :agentId"
                  + " AND e.id.tenancyId = :tenancyId")
          .setParameter("factor", decayFactor)
          .setParameter("agentId", agentId)
          .setParameter("tenancyId", tenancyId)
          .executeUpdate();

        em.createQuery(
                  "DELETE FROM DispositionSignalEntity e"
                  + " WHERE e.id.agentId = :agentId"
                  + " AND e.id.tenancyId = :tenancyId"
                  + " AND e.weight <= 0")
          .setParameter("agentId", agentId)
          .setParameter("tenancyId", tenancyId)
          .executeUpdate();
    }

    @Override
    @Transactional
    public void clear(final String agentId, final String tenancyId) {
        em.createQuery(
                "DELETE FROM DispositionSignalEntity e"
                    + " WHERE e.id.agentId = :agentId"
                    + " AND e.id.tenancyId = :tenancyId")
            .setParameter("agentId", agentId)
            .setParameter("tenancyId", tenancyId)
            .executeUpdate();
    }
}

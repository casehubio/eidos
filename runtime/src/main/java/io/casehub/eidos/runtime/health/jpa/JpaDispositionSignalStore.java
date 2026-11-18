package io.casehub.eidos.runtime.health.jpa;

import io.casehub.eidos.api.DispositionSignalStore;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import java.util.HashMap;
import java.util.Map;

@IfBuildProperty(name = "casehub.eidos.reactive.enabled", stringValue = "false", enableIfMissing = true)
@ApplicationScoped
public class JpaDispositionSignalStore implements DispositionSignalStore {

    @Inject EntityManager em;

    @Override
    @Transactional(TxType.REQUIRES_NEW)
    public void recordActivation(final String agentId, final String tenancyId,
                                 final String functionTerm) {
        int updated = em.createQuery(
                                "UPDATE DispositionSignalEntity e SET e.weight = e.weight + 1"
                                + " WHERE e.id.agentId = :agentId"
                                + " AND e.id.tenancyId = :tenancyId"
                                + " AND e.id.functionTerm = :functionTerm")
                        .setParameter("agentId", agentId)
                        .setParameter("tenancyId", tenancyId)
                        .setParameter("functionTerm", functionTerm)
                        .executeUpdate();
        if (updated == 0) {
            em.persist(new DispositionSignalEntity(agentId, tenancyId, functionTerm, 1.0));
        }
    }

    @Override
    @Transactional(TxType.SUPPORTS)
    public Map<String, Double> activationCounts(final String agentId,
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

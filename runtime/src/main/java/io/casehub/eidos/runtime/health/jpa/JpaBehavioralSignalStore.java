package io.casehub.eidos.runtime.health.jpa;

import io.casehub.eidos.api.BehavioralSignal;
import io.casehub.eidos.api.BehavioralSignalStore;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@IfBuildProperty(name = "casehub.eidos.reactive.enabled", stringValue = "false", enableIfMissing = true)
@ApplicationScoped
public class JpaBehavioralSignalStore implements BehavioralSignalStore {

    @Inject EntityManager em;

    @ConfigProperty(name = "casehub.eidos.behavioral-signal.decline-ttl-days", defaultValue = "30")
    int declineTtlDays;

    @ConfigProperty(name = "casehub.eidos.behavioral-signal.success-ttl-days", defaultValue = "30")
    int successTtlDays;

    @ConfigProperty(name = "casehub.eidos.behavioral-signal.compliant-ttl-days", defaultValue = "30")
    int compliantTtlDays;

    @ConfigProperty(name = "casehub.eidos.behavioral-signal.violated-ttl-days", defaultValue = "90")
    int violatedTtlDays;

    @Override
    @Transactional
    public void record(final String agentId, final String tenancyId,
                       final String capabilityName, final String qualifier,
                       final BehavioralSignal signal) {
        final var id = new BehavioralSignalId(agentId, tenancyId, capabilityName, qualifier, signal.name());
        final var existing = em.find(BehavioralSignalEntity.class, id);
        final var now = Instant.now();
        final var expiresAt = now.plusSeconds((long) ttlDaysFor(signal) * 86400);

        if (existing != null) {
            existing.signalCount++;
            existing.lastRecorded = now;
            existing.expiresAt = expiresAt;
        } else {
            em.persist(new BehavioralSignalEntity(
                agentId, tenancyId, capabilityName, qualifier, signal.name(),
                1, now, expiresAt));
        }
    }

    @Override
    @Transactional
    public void clear(final String agentId, final String tenancyId,
                      final String capabilityName, final BehavioralSignal signal) {
        em.createQuery("DELETE FROM BehavioralSignalEntity e"
                + " WHERE e.id.agentId = :agentId"
                + " AND e.id.tenancyId = :tenancyId"
                + " AND e.id.capabilityName = :capabilityName"
                + " AND e.id.signalType = :signalType")
            .setParameter("agentId", agentId)
            .setParameter("tenancyId", tenancyId)
            .setParameter("capabilityName", capabilityName)
            .setParameter("signalType", signal.name())
            .executeUpdate();
        em.flush();
        em.clear();
    }

    @Override
    @Transactional(TxType.SUPPORTS)
    public Map<String, Integer> learned(final String agentId, final String tenancyId,
                                         final String capabilityName,
                                         final BehavioralSignal signal) {
        final var results = em.createQuery(
                "SELECT e FROM BehavioralSignalEntity e"
                    + " WHERE e.id.agentId = :agentId"
                    + " AND e.id.tenancyId = :tenancyId"
                    + " AND e.id.capabilityName = :capabilityName"
                    + " AND e.id.signalType = :signalType"
                    + " AND e.expiresAt > :now",
                BehavioralSignalEntity.class)
            .setParameter("agentId", agentId)
            .setParameter("tenancyId", tenancyId)
            .setParameter("capabilityName", capabilityName)
            .setParameter("signalType", signal.name())
            .setParameter("now", Instant.now())
            .getResultList();

        final var map = new HashMap<String, Integer>();
        for (final var e : results) {
            map.put(e.id.qualifier, e.signalCount);
        }
        return Map.copyOf(map);
    }

    @Override
    @Transactional(TxType.SUPPORTS)
    public int count(final String agentId, final String tenancyId,
                     final String capabilityName, final String qualifier,
                     final BehavioralSignal signal) {
        final var id = new BehavioralSignalId(agentId, tenancyId, capabilityName, qualifier, signal.name());
        final var entity = em.find(BehavioralSignalEntity.class, id);
        if (entity == null || !Instant.now().isBefore(entity.expiresAt)) return 0;
        return entity.signalCount;
    }

    private int ttlDaysFor(final BehavioralSignal signal) {
        return switch (signal) {
            case DECLINE -> declineTtlDays;
            case SUCCESS -> successTtlDays;
            case COMPLIANT -> compliantTtlDays;
            case VIOLATED -> violatedTtlDays;
        };
    }
}

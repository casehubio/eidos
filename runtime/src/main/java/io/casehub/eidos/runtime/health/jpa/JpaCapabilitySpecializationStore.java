package io.casehub.eidos.runtime.health.jpa;

import io.casehub.eidos.api.CapabilitySpecializationStore;
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
public class JpaCapabilitySpecializationStore implements CapabilitySpecializationStore {

    @Inject EntityManager em;

    @ConfigProperty(name = "casehub.eidos.specialization.decline-ttl-days", defaultValue = "30")
    int declineTtlDays;

    @Override
    @Transactional
    public void recordDecline(String agentId, String tenancyId,
                               String capabilityName, String domain) {
        var id = new CapabilitySpecializationId(agentId, tenancyId, capabilityName, domain);
        var existing = em.find(CapabilitySpecializationEntity.class, id);
        var now = Instant.now();
        var expiresAt = now.plusSeconds((long) declineTtlDays * 86400);

        if (existing != null) {
            existing.declineCount++;
            existing.lastDeclined = now;
            existing.expiresAt = expiresAt;
        } else {
            em.persist(new CapabilitySpecializationEntity(
                agentId, tenancyId, capabilityName, domain, 1, now, expiresAt));
        }
    }

    @Override
    @Transactional
    public void clearDeclines(String agentId, String tenancyId, String capabilityName) {
        em.createQuery("DELETE FROM CapabilitySpecializationEntity e"
                + " WHERE e.id.agentId = :agentId"
                + " AND e.id.tenancyId = :tenancyId"
                + " AND e.id.capabilityName = :capabilityName")
            .setParameter("agentId", agentId)
            .setParameter("tenancyId", tenancyId)
            .setParameter("capabilityName", capabilityName)
            .executeUpdate();
        em.flush();
        em.clear();
    }

    @Override
    @Transactional(TxType.SUPPORTS)
    public Map<String, Integer> learnedExclusions(String agentId, String tenancyId,
                                                    String capabilityName) {
        var results = em.createQuery(
                "SELECT e FROM CapabilitySpecializationEntity e"
                    + " WHERE e.id.agentId = :agentId"
                    + " AND e.id.tenancyId = :tenancyId"
                    + " AND e.id.capabilityName = :capabilityName"
                    + " AND e.expiresAt > :now",
                CapabilitySpecializationEntity.class)
            .setParameter("agentId", agentId)
            .setParameter("tenancyId", tenancyId)
            .setParameter("capabilityName", capabilityName)
            .setParameter("now", Instant.now())
            .getResultList();

        var map = new HashMap<String, Integer>();
        for (var e : results) {
            map.put(e.id.domain, e.declineCount);
        }
        return Map.copyOf(map);
    }

    @Override
    @Transactional(TxType.SUPPORTS)
    public int declineCount(String agentId, String tenancyId,
                             String capabilityName, String domain) {
        var id = new CapabilitySpecializationId(agentId, tenancyId, capabilityName, domain);
        var entity = em.find(CapabilitySpecializationEntity.class, id);
        if (entity == null || !Instant.now().isBefore(entity.expiresAt)) return 0;
        return entity.declineCount;
    }
}

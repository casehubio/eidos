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

import jakarta.annotation.PostConstruct;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@IfBuildProperty(name = "casehub.eidos.reactive.enabled", stringValue = "false", enableIfMissing = true)
@ApplicationScoped
public class JpaBehavioralSignalStore implements BehavioralSignalStore {

    @Inject EntityManager em;
    private boolean       postgres;

    @ConfigProperty(name = "casehub.eidos.behavioral-signal.decline-ttl-days", defaultValue = "30")
    int declineTtlDays;

    @ConfigProperty(name = "casehub.eidos.behavioral-signal.success-ttl-days", defaultValue = "30")
    int successTtlDays;

    @ConfigProperty(name = "casehub.eidos.behavioral-signal.compliant-ttl-days", defaultValue = "30")
    int compliantTtlDays;

    @ConfigProperty(name = "casehub.eidos.behavioral-signal.violated-ttl-days", defaultValue = "90")
    int violatedTtlDays;

    @PostConstruct
    void init() {
        postgres = JpaDialect.isPostgres(em);
    }


    @Override
    @Transactional
    public void record(final String agentId, final String tenancyId,
                       final String capabilityName, final String qualifier,
                       final BehavioralSignal signal) {
        final var now       = Instant.now();
        final var expiresAt = now.plusSeconds((long) ttlDaysFor(signal) * 86400);

        if (postgres) {
            em.createNativeQuery(
                      "INSERT INTO behavioral_signal (agent_id, tenancy_id, capability_name,"
                      + " qualifier, signal_type, signal_count, last_recorded, expires_at)"
                      + " VALUES (:agentId, :tenancyId, :capabilityName, :qualifier, :signalType,"
                      + " 1, :lastRecorded, :expiresAt)"
                      + " ON CONFLICT (agent_id, tenancy_id, capability_name, qualifier, signal_type)"
                      + " DO UPDATE SET signal_count = behavioral_signal.signal_count + 1,"
                      + " last_recorded = EXCLUDED.last_recorded,"
                      + " expires_at = EXCLUDED.expires_at")
              .setParameter("agentId", agentId)
              .setParameter("tenancyId", tenancyId)
              .setParameter("capabilityName", capabilityName)
              .setParameter("qualifier", qualifier)
              .setParameter("signalType", signal.name())
              .setParameter("lastRecorded", now)
              .setParameter("expiresAt", expiresAt)
              .executeUpdate();
        } else {
            em.createNativeQuery(
                      "MERGE INTO behavioral_signal AS bs"
                      + " USING (VALUES (CAST(:agentId AS VARCHAR), CAST(:tenancyId AS VARCHAR),"
                      + " CAST(:capabilityName AS VARCHAR), CAST(:qualifier AS VARCHAR),"
                      + " CAST(:signalType AS VARCHAR),"
                      + " CAST(1 AS INT), CAST(:lastRecorded AS TIMESTAMP WITH TIME ZONE),"
                      + " CAST(:expiresAt AS TIMESTAMP WITH TIME ZONE)))"
                      + " AS src(agent_id, tenancy_id, capability_name, qualifier, signal_type,"
                      + " signal_count, last_recorded, expires_at)"
                      + " ON bs.agent_id = src.agent_id"
                      + " AND bs.tenancy_id = src.tenancy_id"
                      + " AND bs.capability_name = src.capability_name"
                      + " AND bs.qualifier = src.qualifier"
                      + " AND bs.signal_type = src.signal_type"
                      + " WHEN MATCHED THEN UPDATE SET signal_count = bs.signal_count + 1,"
                      + " last_recorded = src.last_recorded,"
                      + " expires_at = src.expires_at"
                      + " WHEN NOT MATCHED THEN INSERT (agent_id, tenancy_id, capability_name,"
                      + " qualifier, signal_type, signal_count, last_recorded, expires_at)"
                      + " VALUES (src.agent_id, src.tenancy_id, src.capability_name, src.qualifier,"
                      + " src.signal_type, src.signal_count, src.last_recorded, src.expires_at)")
              .setParameter("agentId", agentId)
              .setParameter("tenancyId", tenancyId)
              .setParameter("capabilityName", capabilityName)
              .setParameter("qualifier", qualifier)
              .setParameter("signalType", signal.name())
              .setParameter("lastRecorded", now)
              .setParameter("expiresAt", expiresAt)
              .executeUpdate();
        }
        em.flush();
        em.clear();
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

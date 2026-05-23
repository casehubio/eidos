package io.casehub.eidos.runtime.registry.jpa;

import io.casehub.eidos.api.*;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import java.util.List;
import java.util.Optional;

@IfBuildProperty(name = "casehub.eidos.reactive.enabled", stringValue = "false", enableIfMissing = true)
@ApplicationScoped
public class JpaAgentRegistry implements AgentRegistry {

    @Inject EntityManager em;
    @Inject AgentDescriptorMapper mapper;

    @Override
    @Transactional
    public void register(AgentDescriptor descriptor) {
        em.createQuery("DELETE FROM AgentDescriptorEntity e WHERE e.agentId = :id")
          .setParameter("id", descriptor.agentId())
          .executeUpdate();
        // Flush and clear so the bulk delete is visible to the session before persist.
        // Without this, Hibernate's first-level cache still holds the old entity
        // and throws EntityExistsException on the subsequent persist.
        em.flush();
        em.clear();
        em.persist(mapper.toEntity(descriptor));
    }

    @Override
    @Transactional(TxType.SUPPORTS)
    public Optional<AgentDescriptor> findById(String agentId, String tenancyId) {
        return em.createQuery(
                "SELECT DISTINCT a FROM AgentDescriptorEntity a LEFT JOIN FETCH a.capabilities"
                + " WHERE a.agentId = :id AND a.tenancyId = :tenancyId",
                AgentDescriptorEntity.class)
            .setParameter("id", agentId)
            .setParameter("tenancyId", tenancyId)
            .getResultStream()
            .findFirst()
            .map(mapper::toRecord);
    }

    @Override
    @Transactional(TxType.SUPPORTS)
    public List<AgentDescriptor> find(AgentQuery query) {
        String fetchJoin = query.capabilityName() != null
            ? "JOIN FETCH a.capabilities c"
            : "LEFT JOIN FETCH a.capabilities c";

        var jpql = new StringBuilder(
            "SELECT DISTINCT a FROM AgentDescriptorEntity a " + fetchJoin
            + " WHERE a.tenancyId = :tenancyId");
        if (query.slot() != null) jpql.append(" AND a.slot = :slot");
        if (query.capabilityName() != null) jpql.append(" AND c.name = :capabilityName");

        var q = em.createQuery(jpql.toString(), AgentDescriptorEntity.class)
                  .setParameter("tenancyId", query.tenancyId());
        if (query.slot() != null) q.setParameter("slot", query.slot());
        if (query.capabilityName() != null) q.setParameter("capabilityName", query.capabilityName());

        return q.getResultList().stream().map(mapper::toRecord).toList();
    }
}

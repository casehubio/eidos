package io.casehub.eidos.runtime.registry.jpa;

import io.casehub.eidos.api.*;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@IfBuildProperty(name = "casehub.eidos.reactive.enabled", stringValue = "false", enableIfMissing = true)
@ApplicationScoped
public class JpaAgentRegistry implements AgentRegistry {

    @Inject EntityManager em;
    @Inject AgentDescriptorMapper mapper;
    @Inject VocabularyRegistry vocabularyRegistry;

    @Override
    @Transactional
    public void register(AgentDescriptor descriptor) {
        CapabilityVocabularyValidator.validate(descriptor, vocabularyRegistry);
        em.createQuery("DELETE FROM AgentDescriptorEntity e WHERE e.agentId = :id AND e.tenancyId = :tenancyId")
          .setParameter("id", descriptor.agentId())
          .setParameter("tenancyId", descriptor.tenancyId())
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
        Objects.requireNonNull(agentId, "agentId");
        Objects.requireNonNull(tenancyId, "tenancyId");
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
        String fetchJoin = (query.capabilityName() != null || query.taskDomain() != null)
            ? "JOIN FETCH a.capabilities c"
            : "LEFT JOIN FETCH a.capabilities c";

        var jpql = new StringBuilder(
            "SELECT DISTINCT a FROM AgentDescriptorEntity a " + fetchJoin
            + " WHERE a.tenancyId = :tenancyId");
        if (query.slot() != null) jpql.append(" AND a.slot = :slot");

        // Capability matching with vocabulary expansion
        Map<String, Set<String>> capabilityExpansion = null;
        if (query.capabilityName() != null) {
            capabilityExpansion = vocabularyRegistry.expandForMatchingByVocabulary(query.capabilityName());
            if (capabilityExpansion.isEmpty()) {
                // No vocabulary grounding - exact match only
                jpql.append(" AND c.name = :capabilityName");
            } else {
                // Vocabulary grounding - expand to include specialized terms
                jpql.append(" AND (c.name = :capabilityName");
                int idx = 0;
                for (var entry : capabilityExpansion.entrySet()) {
                    jpql.append(" OR (c.capabilityVocabulary = :vocab").append(idx)
                        .append(" AND c.name IN :expanded").append(idx).append(")");
                    idx++;
                }
                jpql.append(")");
            }
        }

        if (query.taskDomain() != null) jpql.append(" AND :taskDomain NOT MEMBER OF c.excludedDomains");

        var q = em.createQuery(jpql.toString(), AgentDescriptorEntity.class)
                  .setParameter("tenancyId", query.tenancyId());
        if (query.slot() != null) q.setParameter("slot", query.slot());

        if (query.capabilityName() != null) {
            q.setParameter("capabilityName", query.capabilityName());
            if (capabilityExpansion != null && !capabilityExpansion.isEmpty()) {
                int idx = 0;
                for (var entry : capabilityExpansion.entrySet()) {
                    q.setParameter("vocab" + idx, entry.getKey());
                    q.setParameter("expanded" + idx, entry.getValue());
                    idx++;
                }
            }
        }

        if (query.taskDomain() != null) q.setParameter("taskDomain", query.taskDomain());

        return q.getResultList().stream().map(mapper::toRecord).toList();
    }
}

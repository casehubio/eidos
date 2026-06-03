package io.casehub.eidos.graph;

import io.casehub.eidos.api.*;
import io.casehub.eidos.graph.entity.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.UUID;
import java.util.List;

@ApplicationScoped
public class JpaAgentGraphStore implements AgentGraphStore {

    @Inject EntityManager em;

    @Override
    @Transactional
    public void recordTask(final AgentTask task) {
        em.persist(AgentTaskEntity.from(task));
    }

    @Override
    @Transactional
    public void recordOutcome(final AgentTaskId id, final AgentOutcome outcome) {
        AgentTaskEntity task = em.find(AgentTaskEntity.class, id.taskId());
        if (task == null) return;
        em.persist(AgentOutcomeEntity.from(outcome, task));
    }

    @Override
    @Transactional
    public void linkAttestation(final AgentTaskId id, final AttestationRef ref) {
        // Idempotent: skip if a ref with this (ledgerEntryHash, tenancyId) already exists.
        // JPQL check-then-insert is safe within @Transactional; avoids dialect-specific
        // ON CONFLICT syntax that H2 in MODE=PostgreSQL does not support.
        List<?> existing = em.createQuery(
                "SELECT a FROM AttestationRefEntity a " +
                "WHERE a.ledgerEntryHash = :hash AND a.tenancyId = :tn")
            .setParameter("hash", ref.ledgerEntryHash())
            .setParameter("tn", ref.tenancyId())
            .setMaxResults(1)
            .getResultList();
        if (!existing.isEmpty()) return;

        AgentTaskEntity task = em.find(AgentTaskEntity.class, id.taskId());
        em.persist(AttestationRefEntity.from(UUID.randomUUID().toString(), ref, task));
    }
}

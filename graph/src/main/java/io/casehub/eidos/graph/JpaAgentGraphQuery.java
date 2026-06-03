package io.casehub.eidos.graph;

import io.casehub.eidos.api.*;
import io.casehub.eidos.graph.entity.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import java.time.Instant;
import java.util.*;
import java.util.stream.*;

@ApplicationScoped
public class JpaAgentGraphQuery implements AgentGraphQuery {

    @Inject EntityManager em;
    @Inject TaskSemanticEnricher enricher;

    // Wilson lower bound: z=1.645
    // quality = confidence × (SUCCEEDED=1.0, PARTIALLY=0.5, FAILED=0.0)
    static double qualityMultiplier(final TaskResult r) {
        return switch (r) { case SUCCEEDED -> 1.0; case PARTIALLY -> 0.5; case FAILED -> 0.0; };
    }

    static double wilsonScore(final double sumQuality, final int n) {
        if (n == 0) return 0.0;
        double p = sumQuality / n;
        double z = 1.645;
        double z2 = z * z;
        double num = p + z2 / (2 * n) - z * Math.sqrt((p * (1 - p) + z2 / (4.0 * n)) / n);
        return num / (1 + z2 / n);
    }

    @Override
    @Transactional(TxType.SUPPORTS)
    public AgentTaskHistory agentHistory(final String agentId, final String tenancyId) {
        List<AgentTaskEntity> tasks = em.createQuery(
                "SELECT t FROM AgentTaskEntity t WHERE t.agentId = :a AND t.tenancyId = :tn",
                AgentTaskEntity.class)
            .setParameter("a", agentId).setParameter("tn", tenancyId)
            .getResultList();

        // Query outcomes directly to avoid stale lazy-load state
        List<AgentOutcome> outcomes = outcomesFor(agentId, tenancyId);

        List<AttestationRef> refs = attestationsFor(agentId, tenancyId);

        int n = outcomes.size();
        Instant from = tasks.stream()
            .map(AgentTaskEntity::startedAt).filter(Objects::nonNull)
            .min(Comparator.naturalOrder()).orElse(null);
        Instant through = tasks.stream()
            .map(AgentTaskEntity::startedAt).filter(Objects::nonNull)
            .max(Comparator.naturalOrder()).orElse(null);

        return new AgentTaskHistory(agentId, tenancyId,
            tasks.stream().map(AgentTaskEntity::toRecord).toList(),
            outcomes, refs,
            GraphDataSufficiency.forCount(n, from, through, List.of()));
    }

    private List<AgentOutcome> outcomesFor(final String agentId, final String tenancyId) {
        return em.createQuery(
                "SELECT o FROM AgentOutcomeEntity o JOIN o.task t " +
                "WHERE t.agentId = :a AND t.tenancyId = :tn",
                AgentOutcomeEntity.class)
            .setParameter("a", agentId).setParameter("tn", tenancyId)
            .getResultList().stream()
            .map(AgentOutcomeEntity::toRecord)
            .toList();
    }

    private List<AgentOutcome> outcomesFor(final String agentId, final String capabilityTag,
                                            final String tenancyId) {
        return em.createQuery(
                "SELECT o FROM AgentOutcomeEntity o JOIN o.task t " +
                "WHERE t.agentId = :a AND t.capabilityTag = :cap AND t.tenancyId = :tn",
                AgentOutcomeEntity.class)
            .setParameter("a", agentId).setParameter("cap", capabilityTag)
            .setParameter("tn", tenancyId)
            .getResultList().stream()
            .map(AgentOutcomeEntity::toRecord)
            .toList();
    }

    @Override
    @Transactional(TxType.SUPPORTS)
    public AgentTaskHistory historyByCapability(final String agentId, final String capabilityTag,
                                                 final String tenancyId) {
        List<AgentTaskEntity> tasks = em.createQuery(
                "SELECT t FROM AgentTaskEntity t WHERE t.agentId = :a " +
                "AND t.capabilityTag = :cap AND t.tenancyId = :tn",
                AgentTaskEntity.class)
            .setParameter("a", agentId).setParameter("cap", capabilityTag)
            .setParameter("tn", tenancyId).getResultList();

        List<AgentOutcome> outcomes = outcomesFor(agentId, capabilityTag, tenancyId);
        List<AttestationRef> refs = attestationsFor(agentId, tenancyId);
        int n = outcomes.size();
        return new AgentTaskHistory(agentId, tenancyId,
            tasks.stream().map(AgentTaskEntity::toRecord).toList(),
            outcomes, refs, GraphDataSufficiency.forCount(n, null, null, List.of()));
    }

    @Override
    @Transactional(TxType.SUPPORTS)
    public List<String> topAgentsByOutcome(final String capabilityTag, final String taskDomain,
                                            final String tenancyId, final int limit) {
        // Collect equivalent domains via enricher
        Set<String> domains = new LinkedHashSet<>();
        domains.add(taskDomain);
        List<String> allDomains = em.createQuery(
                "SELECT DISTINCT t.taskDomain FROM AgentTaskEntity t " +
                "WHERE t.capabilityTag = :cap AND t.tenancyId = :tn",
                String.class)
            .setParameter("cap", capabilityTag).setParameter("tn", tenancyId)
            .getResultList();
        allDomains.stream()
            .filter(d -> d != null && enricher.semanticallyEquivalent(taskDomain, d))
            .forEach(domains::add);

        // Fetch outcomes joined to tasks using JPQL (avoids IN-binding issues with native SQL on H2)
        List<AgentOutcomeEntity> outcomeEntities = em.createQuery(
                "SELECT o FROM AgentOutcomeEntity o JOIN o.task t WHERE t.capabilityTag = :cap " +
                "AND t.tenancyId = :tn AND t.taskDomain IN :domains",
                AgentOutcomeEntity.class)
            .setParameter("cap", capabilityTag)
            .setParameter("tn", tenancyId)
            .setParameter("domains", domains)
            .getResultList();

        // Group by agent and compute Wilson score
        Map<String, double[]> byAgent = new LinkedHashMap<>(); // [sumQuality, count]
        for (AgentOutcomeEntity o : outcomeEntities) {
            String agent = o.task().agentId();
            TaskResult result = TaskResult.valueOf(o.result());
            double conf = o.confidence();
            double q = conf * qualityMultiplier(result);
            byAgent.merge(agent, new double[]{q, 1}, (a, b) -> new double[]{a[0]+b[0], a[1]+b[1]});
        }

        return byAgent.entrySet().stream()
            .sorted(Comparator.comparingDouble(
                (Map.Entry<String, double[]> e) -> wilsonScore(e.getValue()[0], (int) e.getValue()[1])
            ).reversed())
            .limit(limit)
            .map(Map.Entry::getKey)
            .toList();
    }

    @Override
    @Transactional(TxType.SUPPORTS)
    public List<AttestationRef> attestationsFor(final String agentId, final String tenancyId) {
        return em.createQuery(
                "SELECT a FROM AttestationRefEntity a WHERE a.agentId = :ag AND a.tenancyId = :tn",
                AttestationRefEntity.class)
            .setParameter("ag", agentId).setParameter("tn", tenancyId)
            .getResultList().stream()
            .map(AttestationRefEntity::toRecord)
            .toList();
    }
}

package io.casehub.eidos.graph;

import io.casehub.eidos.api.*;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

@QuarkusTest
@TestTransaction
class JpaAgentGraphQueryTest {

    @Inject AgentGraphStore store;
    @Inject AgentGraphQuery query;
    @Inject EntityManager em;

    private void addOutcomes(String agentId, String cap, String domain,
                              int count, TaskResult result, double confidence) {
        for (int i = 0; i < count; i++) {
            String tid = "t-" + agentId + "-" + i;
            store.recordTask(new AgentTask(tid, agentId, "t1", cap, domain,
                                           "ref-" + i, Instant.now(), Instant.now()));
            store.recordOutcome(new AgentTaskId(tid, agentId, "t1"),
                                new AgentOutcome(tid, result, confidence, Instant.now(), null));
        }
    }

    @Test
    void topAgentsByOutcome_wilson_agent_a_beats_b_despite_lower_raw_quality() {
        // Agent A: 20 tasks SUCCEEDED confidence 0.78 → Wilson ≈ 0.60
        addOutcomes("agent-a", "code-review", "rust", 20, TaskResult.SUCCEEDED, 0.78);
        // Agent B:  5 tasks SUCCEEDED confidence 0.90 → Wilson ≈ 0.53
        addOutcomes("agent-b", "code-review", "rust",  5, TaskResult.SUCCEEDED, 0.90);

        List<String> ranked = query.topAgentsByOutcome("code-review", "rust", "t1", 10);

        assertThat(ranked.get(0)).isEqualTo("agent-a");
    }

    @Test
    void topAgentsByOutcome_empty_when_no_history() {
        assertThat(query.topAgentsByOutcome("code-review", "rust", "t1", 5)).isEmpty();
    }

    @Test
    void sufficiency_insufficient_below_5() {
        addOutcomes("a1", "cr", "java", 4, TaskResult.SUCCEEDED, 0.9);
        assertThat(query.agentHistory("a1", "t1").sufficiency().level())
            .isEqualTo(SufficiencyLevel.INSUFFICIENT);
    }

    @Test
    void sufficiency_indicative_at_5() {
        addOutcomes("a2", "cr", "java", 5, TaskResult.SUCCEEDED, 0.9);
        assertThat(query.agentHistory("a2", "t1").sufficiency().level())
            .isEqualTo(SufficiencyLevel.INDICATIVE);
    }

    @Test
    void sufficiency_sufficient_at_10() {
        addOutcomes("a3", "cr", "java", 10, TaskResult.SUCCEEDED, 0.9);
        assertThat(query.agentHistory("a3", "t1").sufficiency().level())
            .isEqualTo(SufficiencyLevel.SUFFICIENT);
    }

    @Test
    void attestationsFor_returns_backfilled_ref_with_null_taskId() {
        em.createNativeQuery(
            "INSERT INTO attestation_ref(ref_id, task_id, agent_id, tenancy_id, " +
            "ledger_entry_hash, entry_type, attested_at) " +
            "VALUES('bf-1', NULL, 'agent-x', 't1', 'hash-backfill', 'ML', now())")
          .executeUpdate();

        List<AttestationRef> refs = query.attestationsFor("agent-x", "t1");

        assertThat(refs).hasSize(1);
        assertThat(refs.get(0).taskId()).isNull();
        assertThat(refs.get(0).ledgerEntryHash()).isEqualTo("hash-backfill");
    }

    @Test
    void historyByCapability_outcomes_scoped_to_that_capability_only() {
        // Agent has outcomes in two capabilities — historyByCapability must return only the queried one.
        // Use capability prefix in IDs to avoid key collision in the same test transaction.
        for (int i = 0; i < 3; i++) {
            String tid = "cr-agent-a-" + i;
            store.recordTask(new AgentTask(tid, "agent-a", "t1", "code-review", "java",
                                           "r", Instant.now(), Instant.now()));
            store.recordOutcome(new AgentTaskId(tid, "agent-a", "t1"),
                                new AgentOutcome(tid, TaskResult.SUCCEEDED, 0.9, Instant.now(), null));
        }
        for (int i = 0; i < 2; i++) {
            String tid = "pl-agent-a-" + i;
            store.recordTask(new AgentTask(tid, "agent-a", "t1", "planning", "agile",
                                           "r", Instant.now(), Instant.now()));
            store.recordOutcome(new AgentTaskId(tid, "agent-a", "t1"),
                                new AgentOutcome(tid, TaskResult.FAILED, 0.1, Instant.now(), null));
        }

        AgentTaskHistory codeReviewHistory = query.historyByCapability("agent-a", "code-review", "t1");
        AgentTaskHistory planningHistory   = query.historyByCapability("agent-a", "planning",    "t1");

        assertThat(codeReviewHistory.tasks()).hasSize(3);
        assertThat(codeReviewHistory.outcomes()).hasSize(3);
        assertThat(codeReviewHistory.outcomes()).allMatch(o -> o.result() == TaskResult.SUCCEEDED);

        assertThat(planningHistory.tasks()).hasSize(2);
        assertThat(planningHistory.outcomes()).hasSize(2);
        assertThat(planningHistory.outcomes()).allMatch(o -> o.result() == TaskResult.FAILED);
    }

    @Test
    void tenancy_isolation_in_ranking() {
        addOutcomes("agent-a", "code-review", "java", 5, TaskResult.SUCCEEDED, 0.9);
        // Different tenancy
        for (int i = 0; i < 5; i++) {
            String tid = "tz-" + i;
            store.recordTask(new AgentTask(tid, "agent-a", "other-tenant",
                "code-review", "java", "r", Instant.now(), Instant.now()));
            store.recordOutcome(new AgentTaskId(tid, "agent-a", "other-tenant"),
                new AgentOutcome(tid, TaskResult.SUCCEEDED, 0.9, Instant.now(), null));
        }
        // Query for t1 only
        List<String> ranked = query.topAgentsByOutcome("code-review", "java", "t1", 10);
        assertThat(ranked).containsExactly("agent-a");
    }
}

package io.casehub.eidos.graph;

import io.casehub.eidos.api.AgentGraphQuery;
import io.casehub.eidos.api.AgentGraphStore;
import io.casehub.eidos.api.AgentOutcome;
import io.casehub.eidos.api.AgentTask;
import io.casehub.eidos.api.AgentTaskHistory;
import io.casehub.eidos.api.AgentTaskId;
import io.casehub.eidos.api.AttestationRef;
import io.casehub.eidos.api.RoleOutcome;
import io.casehub.eidos.api.SufficiencyLevel;
import io.casehub.eidos.api.TaskResult;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
            store.recordTask(new AgentTask(tid, agentId, "t1", cap, domain, null,
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
            store.recordTask(new AgentTask(tid, "agent-a", "t1", "code-review", "java", null,
                                           "r", Instant.now(), Instant.now()));
            store.recordOutcome(new AgentTaskId(tid, "agent-a", "t1"),
                                new AgentOutcome(tid, TaskResult.SUCCEEDED, 0.9, Instant.now(), null));
        }
        for (int i = 0; i < 2; i++) {
            String tid = "pl-agent-a-" + i;
            store.recordTask(new AgentTask(tid, "agent-a", "t1", "planning", "agile", null,
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
                "code-review", "java", null, "r", Instant.now(), Instant.now()));
            store.recordOutcome(new AgentTaskId(tid, "agent-a", "other-tenant"),
                new AgentOutcome(tid, TaskResult.SUCCEEDED, 0.9, Instant.now(), null));
        }
        // Query for t1 only
        List<String> ranked = query.topAgentsByOutcome("code-review", "java", "t1", 10);
        assertThat(ranked).containsExactly("agent-a");
    }

// --- coActiveAgents tests ---


// --- historyByRole tests ---

    @Test
    void historyByRole_returns_only_matching_role_within_window() {
        Instant now           = Instant.now();
        Instant withinWindow  = now.minus(30, java.time.temporal.ChronoUnit.DAYS);
        Instant outsideWindow = now.minus(120, java.time.temporal.ChronoUnit.DAYS);

        // Task with role "coordinator" within window
        store.recordTask(new AgentTask("r1", "agent-r", "t1", "cap", "java", "coordinator",
                                       "ref", withinWindow, withinWindow));
        store.recordOutcome(new AgentTaskId("r1", "agent-r", "t1"),
                            new AgentOutcome("r1", TaskResult.SUCCEEDED, 0.9, withinWindow, null));

        // Task with role "coordinator" outside window
        store.recordTask(new AgentTask("r2", "agent-r", "t1", "cap", "java", "coordinator",
                                       "ref", outsideWindow, outsideWindow));
        store.recordOutcome(new AgentTaskId("r2", "agent-r", "t1"),
                            new AgentOutcome("r2", TaskResult.SUCCEEDED, 0.8, outsideWindow, null));

        // Task with different role within window
        store.recordTask(new AgentTask("r3", "agent-r", "t1", "cap", "java", "analyst",
                                       "ref", withinWindow, withinWindow));
        store.recordOutcome(new AgentTaskId("r3", "agent-r", "t1"),
                            new AgentOutcome("r3", TaskResult.FAILED, 0.5, withinWindow, null));

        // Task with null role within window
        store.recordTask(new AgentTask("r4", "agent-r", "t1", "cap", "java", null,
                                       "ref", withinWindow, withinWindow));

        Instant          since   = now.minus(90, java.time.temporal.ChronoUnit.DAYS);
        AgentTaskHistory history = query.historyByRole("agent-r", "coordinator", "t1", since);

        assertThat(history.tasks()).hasSize(1);
        assertThat(history.tasks().get(0).taskId()).isEqualTo("r1");
        assertThat(history.outcomes()).hasSize(1);
        assertThat(history.outcomes().get(0).taskId()).isEqualTo("r1");
    }

    @Test
    void historyByRole_returns_empty_for_unknown_role() {
        Instant          since   = Instant.now().minus(90, java.time.temporal.ChronoUnit.DAYS);
        AgentTaskHistory history = query.historyByRole("agent-r", "nonexistent", "t1", since);
        assertThat(history.tasks()).isEmpty();
        assertThat(history.outcomes()).isEmpty();
    }

    @Test
    void historyByRole_isolates_by_tenancy() {
        Instant now = Instant.now();
        store.recordTask(new AgentTask("rt1", "agent-r", "tenant-a", "cap", "java", "coordinator",
                                       "ref", now, now));
        store.recordOutcome(new AgentTaskId("rt1", "agent-r", "tenant-a"),
                            new AgentOutcome("rt1", TaskResult.SUCCEEDED, 0.9, now, null));

        Instant          since   = now.minus(90, java.time.temporal.ChronoUnit.DAYS);
        AgentTaskHistory history = query.historyByRole("agent-r", "coordinator", "tenant-b", since);
        assertThat(history.tasks()).isEmpty();
    }

    @Test
    void historyByRole_ordered_by_started_at_descending() {
        Instant now     = Instant.now();
        Instant earlier = now.minus(10, java.time.temporal.ChronoUnit.DAYS);
        Instant later   = now.minus(1, java.time.temporal.ChronoUnit.DAYS);

        store.recordTask(new AgentTask("ro1", "agent-r", "t1", "cap", "java", "coordinator",
                                       "ref", earlier, earlier));
        store.recordTask(new AgentTask("ro2", "agent-r", "t1", "cap", "java", "coordinator",
                                       "ref", later, later));

        Instant          since   = now.minus(90, java.time.temporal.ChronoUnit.DAYS);
        AgentTaskHistory history = query.historyByRole("agent-r", "coordinator", "t1", since);

        assertThat(history.tasks()).hasSize(2);
        assertThat(history.tasks().get(0).taskId()).isEqualTo("ro2");
        assertThat(history.tasks().get(1).taskId()).isEqualTo("ro1");
    }

    @Test
    void historyByRole_with_taskDomain_filters_to_matching_domain() {
        Instant now = Instant.now();

        store.recordTask(new AgentTask("rd1", "agent-r", "t1", "cap", "java", "coordinator",
                                       "ref", now, now));
        store.recordOutcome(new AgentTaskId("rd1", "agent-r", "t1"),
                            new AgentOutcome("rd1", TaskResult.SUCCEEDED, 0.9, now, null));

        store.recordTask(new AgentTask("rd2", "agent-r", "t1", "cap", "python", "coordinator",
                                       "ref", now, now));
        store.recordOutcome(new AgentTaskId("rd2", "agent-r", "t1"),
                            new AgentOutcome("rd2", TaskResult.FAILED, 0.8, now, null));

        store.recordTask(new AgentTask("rd3", "agent-r", "t1", "cap", "rust", "coordinator",
                                       "ref", now, now));
        store.recordOutcome(new AgentTaskId("rd3", "agent-r", "t1"),
                            new AgentOutcome("rd3", TaskResult.SUCCEEDED, 0.7, now, null));

        Instant          since       = now.minus(90, java.time.temporal.ChronoUnit.DAYS);
        AgentTaskHistory javaHistory = query.historyByRole("agent-r", "coordinator", "t1", since, "java");

        assertThat(javaHistory.tasks()).hasSize(1);
        assertThat(javaHistory.tasks().get(0).taskId()).isEqualTo("rd1");
        assertThat(javaHistory.outcomes()).hasSize(1);
        assertThat(javaHistory.outcomes().get(0).result()).isEqualTo(TaskResult.SUCCEEDED);
    }

    @Test
    void historyByRole_with_null_taskDomain_returns_all_domains() {
        Instant now = Instant.now();

        store.recordTask(new AgentTask("rn1", "agent-r", "t1", "cap", "java", "coordinator",
                                       "ref", now, now));
        store.recordOutcome(new AgentTaskId("rn1", "agent-r", "t1"),
                            new AgentOutcome("rn1", TaskResult.SUCCEEDED, 0.9, now, null));

        store.recordTask(new AgentTask("rn2", "agent-r", "t1", "cap", "python", "coordinator",
                                       "ref", now, now));
        store.recordOutcome(new AgentTaskId("rn2", "agent-r", "t1"),
                            new AgentOutcome("rn2", TaskResult.FAILED, 0.8, now, null));

        Instant          since   = now.minus(90, java.time.temporal.ChronoUnit.DAYS);
        AgentTaskHistory history = query.historyByRole("agent-r", "coordinator", "t1", since, null);

        assertThat(history.tasks()).hasSize(2);
        assertThat(history.outcomes()).hasSize(2);
    }

    @Test
    void recordRoleOutcome_and_retrieve_via_historyByRole() {
        Instant now = Instant.now();
        store.recordTask(new AgentTask("ro-t1", "agent-ro", "t1", "cap", "java", "coordinator",
                                       "ref", now, now));
        store.recordOutcome(new AgentTaskId("ro-t1", "agent-ro", "t1"),
                            new AgentOutcome("ro-t1", TaskResult.SUCCEEDED, 0.9, now, null));
        store.recordRoleOutcome(new AgentTaskId("ro-t1", "agent-ro", "t1"),
                                new RoleOutcome("ro-t1", 0.75, "coordination", now));

        Instant          since   = now.minus(90, java.time.temporal.ChronoUnit.DAYS);
        AgentTaskHistory history = query.historyByRole("agent-ro", "coordinator", "t1", since);

        assertThat(history.roleOutcomes()).hasSize(1);
        assertThat(history.roleOutcomes().get(0).roleQuality()).isEqualTo(0.75);
        assertThat(history.roleOutcomes().get(0).dimension()).isEqualTo("coordination");
    }

    @Test
    void historyByRole_returns_empty_roleOutcomes_when_none_recorded() {
        Instant now = Instant.now();
        store.recordTask(new AgentTask("ro-t2", "agent-ro", "t1", "cap", "java", "coordinator",
                                       "ref", now, now));
        store.recordOutcome(new AgentTaskId("ro-t2", "agent-ro", "t1"),
                            new AgentOutcome("ro-t2", TaskResult.SUCCEEDED, 0.9, now, null));

        Instant          since   = now.minus(90, java.time.temporal.ChronoUnit.DAYS);
        AgentTaskHistory history = query.historyByRole("agent-ro", "coordinator", "t1", since);

        assertThat(history.roleOutcomes()).isEmpty();
        assertThat(history.outcomes()).hasSize(1);
    }


    @Test
    void coActiveAgents_returns_agents_with_in_progress_tasks() {
        store.recordTask(new AgentTask("t1", "agent-x", "t1", "cap", "java", null, "case-42", Instant.now(), null));
        store.recordTask(new AgentTask("t2", "agent-y", "t1", "cap", "java", null, "case-42", Instant.now(), null));

        var result = query.coActiveAgents("case-42", "t1");
        assertThat(result).containsExactlyInAnyOrder("agent-x", "agent-y");
    }

    @Test
    void coActiveAgents_excludes_completed_tasks() {
        store.recordTask(new AgentTask("t3", "agent-done", "t1", "cap", "java", null, "case-42", Instant.now(), Instant.now()));
        store.recordTask(new AgentTask("t4", "agent-active", "t1", "cap", "java", null, "case-42", Instant.now(), null));

        var result = query.coActiveAgents("case-42", "t1");
        assertThat(result).containsExactly("agent-active");
    }

    @Test
    void coActiveAgents_isolates_by_tenancy() {
        store.recordTask(new AgentTask("t5", "agent-a", "tenant-a", "cap", "java", null, "case-99", Instant.now(), null));
        store.recordTask(new AgentTask("t6", "agent-b", "tenant-b", "cap", "java", null, "case-99", Instant.now(), null));

        var result = query.coActiveAgents("case-99", "tenant-a");
        assertThat(result).containsExactly("agent-a");
    }

    @Test
    void coActiveAgents_returns_empty_for_unknown_ref() {
        var result = query.coActiveAgents("nonexistent", "t1");
        assertThat(result).isEmpty();
    }
}

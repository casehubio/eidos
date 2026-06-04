package io.casehub.eidos.graph;

import io.casehub.eidos.api.*;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import static org.assertj.core.api.Assertions.*;

@QuarkusTest
@TestTransaction
class JpaAgentGraphStoreTest {

    @Inject AgentGraphStore store;
    @Inject AgentGraphQuery query;

    static AgentTask task(String taskId, String agentId, String cap, String domain) {
        return new AgentTask(taskId, agentId, "t1", cap, domain, "ref-" + taskId,
                             Instant.now(), null);
    }

    @Test
    void recordTask_then_agentHistory_returns_task() {
        store.recordTask(task("t1", "agent-a", "code-review", "java"));

        var history = query.agentHistory("agent-a", "t1");

        assertThat(history.tasks()).hasSize(1);
        assertThat(history.tasks().get(0).taskId()).isEqualTo("t1");
    }

    @Test
    void recordOutcome_links_to_task() {
        Instant before = Instant.now().truncatedTo(ChronoUnit.MICROS);
        store.recordTask(task("t2", "agent-a", "code-review", "java"));
        store.recordOutcome(new AgentTaskId("t2", "agent-a", "t1"),
                            new AgentOutcome("t2", TaskResult.SUCCEEDED, 0.9, before, null));

        var history = query.agentHistory("agent-a", "t1");

        assertThat(history.outcomes()).hasSize(1);
        assertThat(history.outcomes().get(0).result()).isEqualTo(TaskResult.SUCCEEDED);
        assertThat(history.outcomes().get(0).confidence()).isEqualTo(0.9);
        assertThat(history.outcomes().get(0).observedAt()).isEqualTo(before);
    }

    @Test
    void linkAttestation_stored_and_queryable() {
        store.recordTask(task("t3", "agent-a", "code-review", "java"));
        store.linkAttestation(new AgentTaskId("t3", "agent-a", "t1"),
                              new AttestationRef("t3", "agent-a", "t1",
                                  "hash-xyz", "MessageLedgerEntry", Instant.now()));

        var refs = query.attestationsFor("agent-a", "t1");

        assertThat(refs).hasSize(1);
        assertThat(refs.get(0).ledgerEntryHash()).isEqualTo("hash-xyz");
    }

    @Test
    void tenancy_isolation() {
        store.recordTask(new AgentTask("t4", "agent-a", "tenant-A",
            "code-review", "java", "ref", Instant.now(), null));

        assertThat(query.agentHistory("agent-a", "tenant-B").tasks()).isEmpty();
    }

    @Test
    void duplicate_attestation_is_idempotent() {
        store.recordTask(task("t5", "agent-a", "code-review", "java"));
        var ref = new AttestationRef("t5", "agent-a", "t1", "hash-dup", "ML", Instant.now());
        store.linkAttestation(new AgentTaskId("t5", "agent-a", "t1"), ref);
        assertThatCode(() ->
            store.linkAttestation(new AgentTaskId("t5", "agent-a", "t1"), ref)
        ).doesNotThrowAnyException();
        assertThat(query.attestationsFor("agent-a", "t1")).hasSize(1);
    }

    @Test
    void inProgress_tasks_included_in_history() {
        store.recordTask(new AgentTask("t6", "agent-a", "t1",
            "code-review", "java", "ref", Instant.now(), null));
        assertThat(query.agentHistory("agent-a", "t1").tasks()).hasSize(1);
        assertThat(query.agentHistory("agent-a", "t1").tasks().get(0).endedAt()).isNull();
    }
}

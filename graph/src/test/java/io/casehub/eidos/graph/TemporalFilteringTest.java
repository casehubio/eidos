package io.casehub.eidos.graph;

import io.casehub.eidos.api.*;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@TestTransaction
class TemporalFilteringTest {

    @Inject AgentGraphStore store;
    @Inject AgentGraphQuery query;

    private void recordTask(String taskId, String agentId, String tenancyId,
                            String cap, String domain, Instant startedAt) {
        store.recordTask(new AgentTask(taskId, agentId, tenancyId, cap, domain, null,
                "ref", startedAt, startedAt));
        store.recordOutcome(new AgentTaskId(taskId, agentId, tenancyId),
                new AgentOutcome(taskId, TaskResult.SUCCEEDED, 0.9, startedAt, null));
    }

    @Test
    void agentHistory_with_since_excludes_older_tasks() {
        Instant now = Instant.now();
        Instant recent = now.minus(10, ChronoUnit.DAYS);
        Instant old = now.minus(120, ChronoUnit.DAYS);
        Instant since = now.minus(90, ChronoUnit.DAYS);

        recordTask("tf1", "agent-tf", "t1", "cap", "java", recent);
        recordTask("tf2", "agent-tf", "t1", "cap", "java", old);

        var filtered = query.agentHistory("agent-tf", "t1", since);
        assertThat(filtered.tasks()).hasSize(1);
        assertThat(filtered.tasks().get(0).taskId()).isEqualTo("tf1");
        assertThat(filtered.outcomes()).hasSize(1);
    }

    @Test
    void agentHistory_with_null_since_returns_all() {
        Instant now = Instant.now();
        recordTask("tf3", "agent-tf2", "t1", "cap", "java", now.minus(10, ChronoUnit.DAYS));
        recordTask("tf4", "agent-tf2", "t1", "cap", "java", now.minus(200, ChronoUnit.DAYS));

        var all = query.agentHistory("agent-tf2", "t1", null);
        assertThat(all.tasks()).hasSize(2);
    }

    @Test
    void historyByCapability_with_since_excludes_older_tasks() {
        Instant now = Instant.now();
        Instant recent = now.minus(10, ChronoUnit.DAYS);
        Instant old = now.minus(120, ChronoUnit.DAYS);
        Instant since = now.minus(90, ChronoUnit.DAYS);

        recordTask("tf5", "agent-tf3", "t1", "code-review", "java", recent);
        recordTask("tf6", "agent-tf3", "t1", "code-review", "java", old);
        recordTask("tf7", "agent-tf3", "t1", "planning", "java", recent);

        var filtered = query.historyByCapability("agent-tf3", "code-review", "t1", since);
        assertThat(filtered.tasks()).hasSize(1);
        assertThat(filtered.tasks().get(0).taskId()).isEqualTo("tf5");
        assertThat(filtered.outcomes()).hasSize(1);
    }

    @Test
    void historyByCapability_with_null_since_returns_all() {
        Instant now = Instant.now();
        recordTask("tf8", "agent-tf4", "t1", "cap", "java", now.minus(10, ChronoUnit.DAYS));
        recordTask("tf9", "agent-tf4", "t1", "cap", "java", now.minus(200, ChronoUnit.DAYS));

        var all = query.historyByCapability("agent-tf4", "cap", "t1", null);
        assertThat(all.tasks()).hasSize(2);
    }

    @Test
    void agentHistory_without_since_still_works() {
        Instant now = Instant.now();
        recordTask("tf10", "agent-tf5", "t1", "cap", "java", now);

        var history = query.agentHistory("agent-tf5", "t1");
        assertThat(history.tasks()).hasSize(1);
    }

    @Test
    void historyByCapability_without_since_still_works() {
        Instant now = Instant.now();
        recordTask("tf11", "agent-tf6", "t1", "cap", "java", now);

        var history = query.historyByCapability("agent-tf6", "cap", "t1");
        assertThat(history.tasks()).hasSize(1);
    }
}

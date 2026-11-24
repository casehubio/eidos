package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.assertj.core.api.Assertions.assertThat;

class AgentTaskRoleTest {

    @Test
    void agent_task_accepts_role() {
        var task = new AgentTask("t1", "a1", "tenant1", "code-review",
                "java", "coordinator", "ext1", Instant.now(), null);
        assertThat(task.role()).isEqualTo("coordinator");
    }

    @Test
    void agent_task_accepts_null_role() {
        var task = new AgentTask("t1", "a1", "tenant1", "code-review",
                "java", null, "ext1", Instant.now(), null);
        assertThat(task.role()).isNull();
    }
}

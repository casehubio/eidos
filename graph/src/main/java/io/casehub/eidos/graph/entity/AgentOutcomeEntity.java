package io.casehub.eidos.graph.entity;

import io.casehub.eidos.api.AgentOutcome;
import io.casehub.eidos.api.DegradationReason;
import io.casehub.eidos.api.TaskResult;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "agent_outcome")
public class AgentOutcomeEntity {

    @Id
    @Column(name = "task_id")
    String taskId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "task_id")
    AgentTaskEntity task;

    @Column(nullable = false)                String result;
    @Column(nullable = false)                double confidence;
    @Column(name = "degradation_reason")     String degradationReason;
    @Column(name = "observed_at", nullable = false) Instant observedAt;

    protected AgentOutcomeEntity() {}

    public static AgentOutcomeEntity from(final AgentOutcome o, final AgentTaskEntity task) {
        var e = new AgentOutcomeEntity();
        e.task              = task;
        e.taskId            = o.taskId();
        e.result            = o.result().name();
        e.confidence        = o.confidence();
        e.degradationReason = o.degradationReason() != null ? o.degradationReason().name() : null;
        e.observedAt        = Instant.now();
        return e;
    }

    public AgentOutcome toRecord() {
        return new AgentOutcome(
            taskId,
            TaskResult.valueOf(result),
            confidence,
            degradationReason != null ? DegradationReason.valueOf(degradationReason) : null
        );
    }

    public String result()           { return result; }
    public double confidence()       { return confidence; }
    public AgentTaskEntity task()    { return task; }
}

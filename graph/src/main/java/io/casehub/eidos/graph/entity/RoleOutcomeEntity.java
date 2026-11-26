package io.casehub.eidos.graph.entity;

import io.casehub.eidos.api.RoleOutcome;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "role_outcome")
public class RoleOutcomeEntity {

    @Id
    @Column(name = "task_id", length = 36)
    private String taskId;

    @Column(name = "role_quality", nullable = false)
    private double roleQuality;

    @Column(name = "dimension", length = 100)
    private String dimension;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", insertable = false, updatable = false)
    private AgentTaskEntity task;

    protected RoleOutcomeEntity() {}

    public RoleOutcomeEntity(String taskId, double roleQuality, String dimension, Instant observedAt) {
        this.taskId = taskId;
        this.roleQuality = roleQuality;
        this.dimension = dimension;
        this.observedAt = observedAt;
    }

    public static RoleOutcomeEntity from(RoleOutcome record) {
        return new RoleOutcomeEntity(record.taskId(), record.roleQuality(),
                                     record.dimension(), record.observedAt());
    }

    public RoleOutcome toRecord() {
        return new RoleOutcome(taskId, roleQuality, dimension, observedAt);
    }

    public String taskId() { return taskId; }
    public double roleQuality() { return roleQuality; }
    public String dimension() { return dimension; }
    public Instant observedAt() { return observedAt; }
    public AgentTaskEntity task() { return task; }
}

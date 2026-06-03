package io.casehub.eidos.graph.entity;

import io.casehub.eidos.api.AgentTask;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "agent_task")
public class AgentTaskEntity {

    @Id
    @Column(name = "task_id")
    String taskId;

    @Column(name = "agent_id",       nullable = false) String agentId;
    @Column(name = "tenancy_id",     nullable = false) String tenancyId;
    @Column(name = "capability_tag", nullable = false) String capabilityTag;
    @Column(name = "task_domain")                      String taskDomain;
    @Column(name = "external_ref",   columnDefinition = "TEXT") String externalRef;
    @Column(name = "started_at",     nullable = false) Instant startedAt;
    @Column(name = "ended_at")                         Instant endedAt;

    @OneToOne(mappedBy = "task", cascade = CascadeType.ALL,
              fetch = FetchType.LAZY, optional = true)
    AgentOutcomeEntity outcome;

    @OneToMany(mappedBy = "task", fetch = FetchType.LAZY)
    List<AttestationRefEntity> attestationRefs = new ArrayList<>();

    protected AgentTaskEntity() {}

    public static AgentTaskEntity from(final AgentTask t) {
        var e = new AgentTaskEntity();
        e.taskId        = t.taskId();
        e.agentId       = t.agentId();
        e.tenancyId     = t.tenancyId();
        e.capabilityTag = t.capabilityTag();
        e.taskDomain    = t.taskDomain();
        e.externalRef   = t.externalRef();
        e.startedAt     = t.startedAt();
        e.endedAt       = t.endedAt();
        return e;
    }

    public AgentTask toRecord() {
        return new AgentTask(taskId, agentId, tenancyId, capabilityTag,
                             taskDomain, externalRef, startedAt, endedAt);
    }

    public String agentId()              { return agentId; }
    public Instant startedAt()           { return startedAt; }
    public AgentOutcomeEntity outcome()  { return outcome; }
}

package io.casehub.eidos.graph.entity;

import io.casehub.eidos.api.AttestationRef;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "attestation_ref")
public class AttestationRefEntity {

    @Id
    @Column(name = "ref_id")
    String refId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    AgentTaskEntity task;   // nullable for backfilled refs

    @Column(name = "agent_id",          nullable = false) String agentId;
    @Column(name = "tenancy_id",        nullable = false) String tenancyId;
    @Column(name = "ledger_entry_hash", nullable = false) String ledgerEntryHash;
    @Column(name = "entry_type",        nullable = false) String entryType;
    @Column(name = "attested_at",       nullable = false) Instant attestedAt;

    protected AttestationRefEntity() {}

    public static AttestationRefEntity from(final String refId, final AttestationRef ref,
                                             final AgentTaskEntity task) {
        var e = new AttestationRefEntity();
        e.refId           = refId;
        e.task            = task;
        e.agentId         = ref.agentId();
        e.tenancyId       = ref.tenancyId();
        e.ledgerEntryHash = ref.ledgerEntryHash();
        e.entryType       = ref.entryType();
        e.attestedAt      = ref.attestedAt();
        return e;
    }

    public AttestationRef toRecord() {
        return new AttestationRef(
            task != null ? task.taskId : null,
            agentId, tenancyId, ledgerEntryHash, entryType, attestedAt
        );
    }
}

package io.casehub.eidos.runtime.health.jpa;

import jakarta.persistence.*;

@Entity
@Table(name = "disposition_signal")
public class DispositionSignalEntity {

    @EmbeddedId
    DispositionSignalId id;

    @Column(name = "count", nullable = false)
    int count;

    protected DispositionSignalEntity() {}

    DispositionSignalEntity(final String agentId, final String tenancyId,
                            final String functionTerm, final int count) {
        this.id = new DispositionSignalId(agentId, tenancyId, functionTerm);
        this.count = count;
    }
}

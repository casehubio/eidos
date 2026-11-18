package io.casehub.eidos.runtime.health.jpa;

import jakarta.persistence.*;

@Entity
@Table(name = "disposition_signal")
public class DispositionSignalEntity {

    @EmbeddedId
    DispositionSignalId id;

    @Column(name = "weight", nullable = false)
    double weight;

    protected DispositionSignalEntity() {}

    DispositionSignalEntity(final String agentId, final String tenancyId,
                            final String functionTerm, final double weight) {
        this.id     = new DispositionSignalId(agentId, tenancyId, functionTerm);
        this.weight = weight;
    }
}

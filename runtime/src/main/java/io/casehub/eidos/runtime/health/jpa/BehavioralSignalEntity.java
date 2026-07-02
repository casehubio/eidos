package io.casehub.eidos.runtime.health.jpa;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "behavioral_signal")
public class BehavioralSignalEntity {

    @EmbeddedId
    BehavioralSignalId id;

    @Column(name = "signal_count", nullable = false)
    int signalCount;

    @Column(name = "last_recorded", nullable = false)
    Instant lastRecorded;

    @Column(name = "expires_at", nullable = false)
    Instant expiresAt;

    protected BehavioralSignalEntity() {}

    BehavioralSignalEntity(final String agentId, final String tenancyId,
                           final String capabilityName, final String qualifier,
                           final String signalType,
                           final int signalCount, final Instant lastRecorded,
                           final Instant expiresAt) {
        this.id = new BehavioralSignalId(agentId, tenancyId, capabilityName, qualifier, signalType);
        this.signalCount = signalCount;
        this.lastRecorded = lastRecorded;
        this.expiresAt = expiresAt;
    }
}

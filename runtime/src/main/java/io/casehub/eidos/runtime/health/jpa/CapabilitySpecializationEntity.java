package io.casehub.eidos.runtime.health.jpa;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "capability_specialization")
public class CapabilitySpecializationEntity {

    @EmbeddedId
    CapabilitySpecializationId id;

    @Column(name = "signal_count", nullable = false)
    int signalCount;

    @Column(name = "last_recorded", nullable = false)
    Instant lastRecorded;

    @Column(name = "expires_at", nullable = false)
    Instant expiresAt;

    protected CapabilitySpecializationEntity() {}

    CapabilitySpecializationEntity(final String agentId, final String tenancyId,
                                    final String capabilityName, final String domain,
                                    final String signalType,
                                    final int signalCount, final Instant lastRecorded,
                                    final Instant expiresAt) {
        this.id = new CapabilitySpecializationId(agentId, tenancyId, capabilityName, domain, signalType);
        this.signalCount = signalCount;
        this.lastRecorded = lastRecorded;
        this.expiresAt = expiresAt;
    }
}

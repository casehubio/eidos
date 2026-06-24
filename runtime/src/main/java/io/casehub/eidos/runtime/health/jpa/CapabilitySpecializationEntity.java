package io.casehub.eidos.runtime.health.jpa;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "capability_specialization")
public class CapabilitySpecializationEntity {

    @EmbeddedId
    CapabilitySpecializationId id;

    @Column(name = "decline_count", nullable = false)
    int declineCount;

    @Column(name = "last_declined", nullable = false)
    Instant lastDeclined;

    @Column(name = "expires_at", nullable = false)
    Instant expiresAt;

    protected CapabilitySpecializationEntity() {}

    CapabilitySpecializationEntity(String agentId, String tenancyId,
                                    String capabilityName, String domain,
                                    int declineCount, Instant lastDeclined,
                                    Instant expiresAt) {
        this.id = new CapabilitySpecializationId(agentId, tenancyId, capabilityName, domain);
        this.declineCount = declineCount;
        this.lastDeclined = lastDeclined;
        this.expiresAt = expiresAt;
    }
}

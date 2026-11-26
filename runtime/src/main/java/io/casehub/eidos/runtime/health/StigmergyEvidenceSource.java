package io.casehub.eidos.runtime.health;

import io.casehub.eidos.api.CapabilityHealth.ProbeContext;
import io.casehub.eidos.api.DetectedRoleSignalStore;
import io.casehub.eidos.api.RoleEvidence;
import io.casehub.eidos.api.RoleEvidenceSource;
import io.casehub.eidos.api.TemporalDecay;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@ApplicationScoped
public class StigmergyEvidenceSource implements RoleEvidenceSource {

    private final DetectedRoleSignalStore store;
    private final int windowDays;
    private final double halfLifeDays;
    private final double weight;

    @Inject
    public StigmergyEvidenceSource(
            final DetectedRoleSignalStore store,
            @ConfigProperty(name = "casehub.eidos.stigmergy.window-days",
                            defaultValue = "90") final int windowDays,
            @ConfigProperty(name = "casehub.eidos.stigmergy.decay-half-life-days",
                            defaultValue = "90") final double halfLifeDays,
            @ConfigProperty(name = "casehub.eidos.stigmergy.evidence-weight",
                            defaultValue = "0.5") final double weight) {
        if (weight <= 0.0 || Double.isNaN(weight) || Double.isInfinite(weight))
            throw new IllegalArgumentException("casehub.eidos.stigmergy.evidence-weight must be positive and finite, got: " + weight);
        this.store = store;
        this.windowDays = windowDays;
        this.halfLifeDays = halfLifeDays;
        this.weight = weight;
    }

    @Override
    public String sourceId() {
        return "stigmergy";
    }

    @Override
    public RoleEvidence evaluate(final String agentId, final String roleName,
                                  final String tenancyId, final ProbeContext context) {
        final Instant since = Instant.now().minus(windowDays, ChronoUnit.DAYS);
        final var signals = store.signals(agentId, roleName, tenancyId, since);

        final Instant now = Instant.now();
        double qualitySum = 0.0;
        double effectiveCount = 0.0;

        for (final var signal : signals) {
            final double decay = TemporalDecay.exponential(signal.observedAt(), now, halfLifeDays);
            qualitySum += signal.confidence() * decay;
            effectiveCount += decay;
        }

        return new RoleEvidence(sourceId(), qualitySum, effectiveCount, weight, since);
    }
}

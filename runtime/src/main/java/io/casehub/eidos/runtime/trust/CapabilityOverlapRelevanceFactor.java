package io.casehub.eidos.runtime.trust;

import io.casehub.ledger.api.spi.AttestationRelevanceContext;
import io.casehub.ledger.api.spi.AttestationRelevanceFactor;
import io.casehub.ledger.api.spi.RelevanceAssessment;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CapabilityOverlapRelevanceFactor implements AttestationRelevanceFactor {

    static final String SNAPSHOT_KEY = "capability-overlap";
    private static final double BASELINE = 0.5;

    @Override
    public RelevanceAssessment assess(AttestationRelevanceContext ctx) {
        if (ctx.snapshot() == null) return RelevanceAssessment.NEUTRAL;
        Object raw = ctx.snapshot().get(SNAPSHOT_KEY);
        if (raw == null) return RelevanceAssessment.NEUTRAL;
        if (!(raw instanceof Number num)) return RelevanceAssessment.NEUTRAL;

        double overlap = num.doubleValue();
        double weight = BASELINE + (1.0 - BASELINE) * overlap;
        return new RelevanceAssessment(weight,
                SNAPSHOT_KEY + ":" + overlap + ", weight:" + weight);
    }

    @Override
    public String name() {
        return "capability-overlap";
    }
}

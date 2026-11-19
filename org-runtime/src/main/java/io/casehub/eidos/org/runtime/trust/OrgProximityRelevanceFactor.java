package io.casehub.eidos.org.runtime.trust;

import io.casehub.ledger.api.spi.AttestationRelevanceContext;
import io.casehub.ledger.api.spi.AttestationRelevanceFactor;
import io.casehub.ledger.api.spi.RelevanceAssessment;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;

@ApplicationScoped
public class OrgProximityRelevanceFactor implements AttestationRelevanceFactor {

    static final String SNAPSHOT_KEY = "org-distance";

    private final List<Double> weights;

    public OrgProximityRelevanceFactor(
            @ConfigProperty(name = "casehub.eidos.trust.relevance.weights",
                    defaultValue = "1.0,0.7,0.65,0.6")
            List<Double> weights) {
        if (weights.isEmpty()) {
            throw new IllegalArgumentException("Weight table must not be empty");
        }
        for (double w : weights) {
            if (Double.isNaN(w) || w < 0.0 || w > 1.0) {
                throw new IllegalArgumentException(
                        "All weights must be in [0.0, 1.0], got: " + w);
            }
        }
        this.weights = List.copyOf(weights);
    }

    @Override
    public RelevanceAssessment assess(AttestationRelevanceContext ctx) {
        var snapshot = ctx.snapshot();
        if (snapshot == null || !snapshot.containsKey(SNAPSHOT_KEY)) {
            return RelevanceAssessment.NEUTRAL;
        }

        Object raw = snapshot.get(SNAPSHOT_KEY);
        if (!(raw instanceof Number)) {
            return RelevanceAssessment.NEUTRAL;
        }
        int distance = ((Number) raw).intValue();

        if (distance <= 0) {
            return RelevanceAssessment.NEUTRAL;
        }

        int index = Math.min(distance, weights.size()) - 1;
        double weight = weights.get(index);
        return new RelevanceAssessment(weight,
                "org-distance:" + distance + ", weight:" + weight);
    }

    @Override
    public String name() {
        return "org-proximity";
    }
}

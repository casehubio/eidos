package io.casehub.eidos.org.runtime.trust;

import io.casehub.ledger.api.spi.AttestationRelevanceContext;
import io.casehub.ledger.api.spi.AttestationRelevanceFactor;
import io.casehub.ledger.api.spi.RelevanceAssessment;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class IndependenceAwareRelevanceFactor implements AttestationRelevanceFactor {

    private final double maxSameUnitShare;

    public IndependenceAwareRelevanceFactor(
            @ConfigProperty(name = "casehub.eidos.trust.independence.max-same-unit-share",
                            defaultValue = "0.6")
            double maxSameUnitShare) {
        if (Double.isNaN(maxSameUnitShare) || maxSameUnitShare <= 0.0 || maxSameUnitShare > 1.0) {
            throw new IllegalArgumentException(
                    "maxSameUnitShare must be in (0.0, 1.0], got: " + maxSameUnitShare);
        }
        this.maxSameUnitShare = maxSameUnitShare;
    }

    @Override
    public RelevanceAssessment assess(AttestationRelevanceContext ctx) {
        return RelevanceAssessment.NEUTRAL;
    }

    @Override
    public Map<UUID, RelevanceAssessment> assessBatch(List<AttestationRelevanceContext> ctxs) {
        Map<UUID, RelevanceAssessment> result = new LinkedHashMap<>();
        if (ctxs.isEmpty()) {
            return result;
        }

        List<UUID> sameUnit = new ArrayList<>();
        List<UUID> crossUnit = new ArrayList<>();
        List<UUID> unknown = new ArrayList<>();

        for (AttestationRelevanceContext ctx : ctxs) {
            int distance = OrgDistanceUtil.extractDistance(ctx.snapshot());
            if (distance == 1) {
                sameUnit.add(ctx.attestationId());
            } else if (distance > 1) {
                crossUnit.add(ctx.attestationId());
            } else {
                unknown.add(ctx.attestationId());
            }
        }

        int sameCount = sameUnit.size();
        int crossCount = crossUnit.size();

        if (crossCount == 0 || sameCount == 0) {
            for (AttestationRelevanceContext ctx : ctxs) {
                result.put(ctx.attestationId(), RelevanceAssessment.NEUTRAL);
            }
            return result;
        }

        double fraction = (double) sameCount / (sameCount + crossCount);

        if (fraction <= maxSameUnitShare) {
            for (AttestationRelevanceContext ctx : ctxs) {
                result.put(ctx.attestationId(), RelevanceAssessment.NEUTRAL);
            }
            return result;
        }

        double weight = maxSameUnitShare * crossCount
                / ((1.0 - maxSameUnitShare) * sameCount);
        int knownCount = sameCount + crossCount;
        long capPercent = Math.round(maxSameUnitShare * 100);
        long sharePercent = Math.round(fraction * 100);
        String reason = "independence: same-unit " + sameCount + "/" + knownCount
                + " (" + sharePercent + "%) > cap " + capPercent + "%, weight: " + weight;

        RelevanceAssessment capped = new RelevanceAssessment(weight, reason);

        for (UUID id : sameUnit) {
            result.put(id, capped);
        }
        for (UUID id : crossUnit) {
            result.put(id, RelevanceAssessment.NEUTRAL);
        }
        for (UUID id : unknown) {
            result.put(id, RelevanceAssessment.NEUTRAL);
        }

        return result;
    }

    @Override
    public String name() {
        return "independence";
    }


}

package io.casehub.eidos.org.runtime.trust;

import io.casehub.eidos.api.CrossUnitEvidenceAssessment;
import io.casehub.eidos.api.CrossUnitEvidenceAssessor;
import io.casehub.eidos.api.EvidenceDiversityLevel;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class DefaultCrossUnitEvidenceAssessor implements CrossUnitEvidenceAssessor {

    private final int minCrossUnit;

    public DefaultCrossUnitEvidenceAssessor(
            @ConfigProperty(name = "casehub.eidos.trust.diversity.min-cross-unit",
                            defaultValue = "2")
            int minCrossUnit) {
        if (minCrossUnit < 1) {
            throw new IllegalArgumentException(
                    "minCrossUnit must be >= 1, got: " + minCrossUnit);
        }
        this.minCrossUnit = minCrossUnit;
    }

    @Override
    public CrossUnitEvidenceAssessment assess(List<Map<String, Object>> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            return CrossUnitEvidenceAssessment.UNKNOWN;
        }

        int sameUnit = 0, crossUnit = 0, unknown = 0;

        for (Map<String, Object> snapshot : snapshots) {
            int distance = OrgDistanceUtil.extractDistance(snapshot);
            if (distance == 1) {
                sameUnit++;
            } else if (distance > 1) {
                crossUnit++;
            } else {
                unknown++;
            }
        }

        if (sameUnit == 0 && crossUnit == 0) {
            return new CrossUnitEvidenceAssessment(0, 0, unknown,
                    EvidenceDiversityLevel.UNKNOWN, List.of());
        }

        EvidenceDiversityLevel level = crossUnit >= minCrossUnit
                ? EvidenceDiversityLevel.DIVERSE
                : EvidenceDiversityLevel.HOMOGENEOUS;

        List<String> warnings = new ArrayList<>();
        if (level == EvidenceDiversityLevel.HOMOGENEOUS) {
            int total = sameUnit + crossUnit;
            warnings.add("cross-unit: " + crossUnit + "/" + total
                    + " (threshold: " + minCrossUnit + ")");
        }

        return new CrossUnitEvidenceAssessment(sameUnit, crossUnit, unknown,
                level, warnings);
    }
}

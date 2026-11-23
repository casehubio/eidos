package io.casehub.eidos.core.trust;

import io.casehub.eidos.api.CrossUnitEvidenceAssessment;
import io.casehub.eidos.api.CrossUnitEvidenceAssessor;

import java.util.List;
import java.util.Map;

public class NoOpCrossUnitEvidenceAssessor implements CrossUnitEvidenceAssessor {

    @Override
    public CrossUnitEvidenceAssessment assess(List<Map<String, Object>> snapshots) {
        return CrossUnitEvidenceAssessment.UNKNOWN;
    }
}

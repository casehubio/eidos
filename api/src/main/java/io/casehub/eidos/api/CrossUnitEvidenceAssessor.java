package io.casehub.eidos.api;

import java.util.List;
import java.util.Map;

public interface CrossUnitEvidenceAssessor {

    CrossUnitEvidenceAssessment assess(List<Map<String, Object>> snapshots);
}

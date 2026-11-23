package io.casehub.eidos.core.trust;

import io.casehub.eidos.api.CrossUnitEvidenceAssessment;
import io.casehub.eidos.api.EvidenceDiversityLevel;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class NoOpCrossUnitEvidenceAssessorTest {

    private final NoOpCrossUnitEvidenceAssessor assessor = new NoOpCrossUnitEvidenceAssessor();

    @Test
    void returns_unknown_for_null() {
        var result = assessor.assess(null);
        assertThat(result).isEqualTo(CrossUnitEvidenceAssessment.UNKNOWN);
    }

    @Test
    void returns_unknown_for_populated_snapshots() {
        var result = assessor.assess(List.of(Map.of("org-distance", 1)));
        assertThat(result.level()).isEqualTo(EvidenceDiversityLevel.UNKNOWN);
    }
}

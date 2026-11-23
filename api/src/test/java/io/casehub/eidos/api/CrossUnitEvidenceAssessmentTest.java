package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class CrossUnitEvidenceAssessmentTest {

    @Test
    void valid_construction() {
        var assessment = new CrossUnitEvidenceAssessment(5, 3, 1,
                EvidenceDiversityLevel.DIVERSE, List.of());
        assertThat(assessment.sameUnitCount()).isEqualTo(5);
        assertThat(assessment.crossUnitCount()).isEqualTo(3);
        assertThat(assessment.unknownCount()).isEqualTo(1);
        assertThat(assessment.level()).isEqualTo(EvidenceDiversityLevel.DIVERSE);
        assertThat(assessment.warnings()).isEmpty();
    }

    @Test
    void negative_same_unit_count_rejected() {
        assertThatThrownBy(() -> new CrossUnitEvidenceAssessment(-1, 0, 0,
                EvidenceDiversityLevel.UNKNOWN, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void negative_cross_unit_count_rejected() {
        assertThatThrownBy(() -> new CrossUnitEvidenceAssessment(0, -1, 0,
                EvidenceDiversityLevel.UNKNOWN, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void negative_unknown_count_rejected() {
        assertThatThrownBy(() -> new CrossUnitEvidenceAssessment(0, 0, -1,
                EvidenceDiversityLevel.UNKNOWN, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void warnings_are_immutable() {
        var mutable = new ArrayList<>(List.of("warning"));
        var assessment = new CrossUnitEvidenceAssessment(1, 0, 0,
                EvidenceDiversityLevel.HOMOGENEOUS, mutable);
        assertThatThrownBy(() -> assessment.warnings().add("another"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void unknown_constant_has_zero_counts() {
        var unknown = CrossUnitEvidenceAssessment.UNKNOWN;
        assertThat(unknown.sameUnitCount()).isEqualTo(0);
        assertThat(unknown.crossUnitCount()).isEqualTo(0);
        assertThat(unknown.unknownCount()).isEqualTo(0);
        assertThat(unknown.level()).isEqualTo(EvidenceDiversityLevel.UNKNOWN);
        assertThat(unknown.warnings()).isEmpty();
    }
}

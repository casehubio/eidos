package io.casehub.eidos.org.runtime.trust;

import io.casehub.eidos.api.EvidenceDiversityLevel;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class DefaultCrossUnitEvidenceAssessorTest {

    private final DefaultCrossUnitEvidenceAssessor assessor =
            new DefaultCrossUnitEvidenceAssessor(2);

    @Test
    void null_snapshots_returns_unknown() {
        var result = assessor.assess(null);
        assertThat(result.level()).isEqualTo(EvidenceDiversityLevel.UNKNOWN);
    }

    @Test
    void empty_snapshots_returns_unknown() {
        var result = assessor.assess(List.of());
        assertThat(result.level()).isEqualTo(EvidenceDiversityLevel.UNKNOWN);
    }

    @Test
    void all_same_unit_returns_homogeneous() {
        var snapshots = List.of(
                Map.<String, Object>of("org-distance", 1),
                Map.<String, Object>of("org-distance", 1),
                Map.<String, Object>of("org-distance", 1));
        var result = assessor.assess(snapshots);
        assertThat(result.level()).isEqualTo(EvidenceDiversityLevel.HOMOGENEOUS);
        assertThat(result.sameUnitCount()).isEqualTo(3);
        assertThat(result.crossUnitCount()).isEqualTo(0);
        assertThat(result.warnings()).hasSize(1);
    }

    @Test
    void cross_unit_at_threshold_returns_diverse() {
        var snapshots = List.of(
                Map.<String, Object>of("org-distance", 1),
                Map.<String, Object>of("org-distance", 2),
                Map.<String, Object>of("org-distance", 3));
        var result = assessor.assess(snapshots);
        assertThat(result.level()).isEqualTo(EvidenceDiversityLevel.DIVERSE);
        assertThat(result.sameUnitCount()).isEqualTo(1);
        assertThat(result.crossUnitCount()).isEqualTo(2);
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    void cross_unit_below_threshold_returns_homogeneous() {
        var snapshots = List.of(
                Map.<String, Object>of("org-distance", 1),
                Map.<String, Object>of("org-distance", 1),
                Map.<String, Object>of("org-distance", 2));
        var result = assessor.assess(snapshots);
        assertThat(result.level()).isEqualTo(EvidenceDiversityLevel.HOMOGENEOUS);
        assertThat(result.crossUnitCount()).isEqualTo(1);
    }

    @Test
    void all_unknown_snapshots_returns_unknown() {
        var snapshots = List.of(
                Map.<String, Object>of("other-key", "value"),
                Map.<String, Object>of("other-key", "value"));
        var result = assessor.assess(snapshots);
        assertThat(result.level()).isEqualTo(EvidenceDiversityLevel.UNKNOWN);
        assertThat(result.unknownCount()).isEqualTo(2);
    }

    @Test
    void mixed_known_and_unknown_uses_known_counts() {
        var snapshots = List.of(
                Map.<String, Object>of("org-distance", 1),
                Map.<String, Object>of("org-distance", 2),
                Map.<String, Object>of("org-distance", 3),
                Map.<String, Object>of("other-key", "value"));
        var result = assessor.assess(snapshots);
        assertThat(result.level()).isEqualTo(EvidenceDiversityLevel.DIVERSE);
        assertThat(result.unknownCount()).isEqualTo(1);
        assertThat(result.crossUnitCount()).isEqualTo(2);
    }

    @Test
    void null_entries_in_list_treated_as_unknown() {
        List<Map<String, Object>> snapshots = new ArrayList<>();
        snapshots.add(Map.of("org-distance", 1));
        snapshots.add(null);
        snapshots.add(Map.of("org-distance", 2));
        snapshots.add(Map.of("org-distance", 3));
        var result = assessor.assess(snapshots);
        assertThat(result.level()).isEqualTo(EvidenceDiversityLevel.DIVERSE);
        assertThat(result.unknownCount()).isEqualTo(1);
    }

    @Test
    void threshold_one_makes_single_cross_unit_sufficient() {
        var assessor1 = new DefaultCrossUnitEvidenceAssessor(1);
        var snapshots = List.of(
                Map.<String, Object>of("org-distance", 1),
                Map.<String, Object>of("org-distance", 2));
        var result = assessor1.assess(snapshots);
        assertThat(result.level()).isEqualTo(EvidenceDiversityLevel.DIVERSE);
    }

    @Test
    void invalid_threshold_rejected() {
        assertThatThrownBy(() -> new DefaultCrossUnitEvidenceAssessor(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DefaultCrossUnitEvidenceAssessor(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void homogeneous_warning_includes_counts_and_threshold() {
        var snapshots = List.of(
                Map.<String, Object>of("org-distance", 1),
                Map.<String, Object>of("org-distance", 1));
        var result = assessor.assess(snapshots);
        assertThat(result.warnings().get(0))
                .contains("cross-unit: 0/2")
                .contains("threshold: 2");
    }

    @Test
    void all_cross_unit_returns_diverse() {
        var snapshots = List.of(
                Map.<String, Object>of("org-distance", 2),
                Map.<String, Object>of("org-distance", 3));
        var result = assessor.assess(snapshots);
        assertThat(result.level()).isEqualTo(EvidenceDiversityLevel.DIVERSE);
        assertThat(result.sameUnitCount()).isEqualTo(0);
    }
}

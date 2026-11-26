package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;

class RoleEvidenceTest {

    @Test
    void valid_evidence() {
        var e = new RoleEvidence("task-outcome", 4.5, 10.0, 1.0, Instant.now());
        assertThat(e.sourceId()).isEqualTo("task-outcome");
        assertThat(e.qualitySum()).isEqualTo(4.5);
        assertThat(e.effectiveCount()).isEqualTo(10.0);
    }

    @Test
    void rejects_zero_weight() {
        assertThatThrownBy(() -> new RoleEvidence("s", 1.0, 1.0, 0.0, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_negative_weight() {
        assertThatThrownBy(() -> new RoleEvidence("s", 1.0, 1.0, -1.0, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_nan_weight() {
        assertThatThrownBy(() -> new RoleEvidence("s", 1.0, 1.0, Double.NaN, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_infinite_weight() {
        assertThatThrownBy(() -> new RoleEvidence("s", 1.0, 1.0, Double.POSITIVE_INFINITY, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_negative_quality_sum() {
        assertThatThrownBy(() -> new RoleEvidence("s", -1.0, 1.0, 1.0, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_negative_effective_count() {
        assertThatThrownBy(() -> new RoleEvidence("s", 1.0, -1.0, 1.0, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void zero_evidence_is_valid() {
        var e = new RoleEvidence("s", 0.0, 0.0, 1.0, Instant.now());
        assertThat(e.effectiveCount()).isEqualTo(0.0);
        assertThat(e.qualitySum()).isEqualTo(0.0);
    }

    @Test
    void rejects_null_source_id() {
        assertThatThrownBy(() -> new RoleEvidence(null, 1.0, 1.0, 1.0, Instant.now()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_null_window_start() {
        assertThatThrownBy(() -> new RoleEvidence("s", 1.0, 1.0, 1.0, null))
                .isInstanceOf(NullPointerException.class);
    }
}

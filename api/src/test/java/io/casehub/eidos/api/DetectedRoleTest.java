package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DetectedRoleTest {

    private static final Instant T1 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant T2 = Instant.parse("2026-06-01T00:00:00Z");

    @Test
    void valid_detected_role() {
        var role = new DetectedRole("coordinator", 0.85, 12, 8.5, T1, T2,
            List.of("delegation", "review-initiation"));
        assertThat(role.roleName()).isEqualTo("coordinator");
        assertThat(role.signalCount()).isEqualTo(12);
        assertThat(role.patternTypes()).containsExactly("delegation", "review-initiation");
    }

    @Test
    void pattern_types_defensively_copied() {
        var types = new ArrayList<>(List.of("delegation"));
        var role = new DetectedRole("coordinator", 0.85, 5, 4.0, T1, T2, types);
        types.add("should-not-appear");
        assertThat(role.patternTypes()).hasSize(1);
    }

    @Test
    void rejects_null_roleName() {
        assertThatThrownBy(() -> new DetectedRole(null, 0.5, 5, 4.0, T1, T2, List.of()))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_negative_confidence() {
        assertThatThrownBy(() -> new DetectedRole("x", -0.1, 5, 4.0, T1, T2, List.of()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_negative_signalCount() {
        assertThatThrownBy(() -> new DetectedRole("x", 0.5, -1, 4.0, T1, T2, List.of()))
            .isInstanceOf(IllegalArgumentException.class);
    }
}

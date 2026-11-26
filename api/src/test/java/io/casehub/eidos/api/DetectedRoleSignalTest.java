package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DetectedRoleSignalTest {

    private static final Instant NOW = Instant.now();

    @Test
    void valid_signal_all_fields() {
        var signal = new DetectedRoleSignal(
            "agent-1", "tenant-1", "coordinator", "delegation",
            "urn:casehub:stigmergy-patterns", 0.85, NOW);
        assertThat(signal.agentId()).isEqualTo("agent-1");
        assertThat(signal.roleName()).isEqualTo("coordinator");
        assertThat(signal.patternVocabulary()).isEqualTo("urn:casehub:stigmergy-patterns");
    }

    @Test
    void valid_signal_null_vocabulary() {
        var signal = new DetectedRoleSignal(
            "agent-1", "tenant-1", "reviewer", "review-initiation",
            null, 0.5, NOW);
        assertThat(signal.patternVocabulary()).isNull();
    }

    @Test
    void rejects_null_agentId() {
        assertThatThrownBy(() -> new DetectedRoleSignal(
            null, "tenant-1", "coordinator", "delegation", null, 0.5, NOW))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_blank_roleName() {
        assertThatThrownBy(() -> new DetectedRoleSignal(
            "agent-1", "tenant-1", "  ", "delegation", null, 0.5, NOW))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_confidence_below_zero() {
        assertThatThrownBy(() -> new DetectedRoleSignal(
            "agent-1", "tenant-1", "coordinator", "delegation", null, -0.1, NOW))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_confidence_above_one() {
        assertThatThrownBy(() -> new DetectedRoleSignal(
            "agent-1", "tenant-1", "coordinator", "delegation", null, 1.1, NOW))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_NaN_confidence() {
        assertThatThrownBy(() -> new DetectedRoleSignal(
            "agent-1", "tenant-1", "coordinator", "delegation", null, Double.NaN, NOW))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_null_observedAt() {
        assertThatThrownBy(() -> new DetectedRoleSignal(
            "agent-1", "tenant-1", "coordinator", "delegation", null, 0.5, null))
            .isInstanceOf(NullPointerException.class);
    }
}

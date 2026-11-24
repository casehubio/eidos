package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WilsonConfidenceTest {

    @Test
    void zero_observations_returns_zero() {
        assertThat(WilsonConfidence.score(0.0, 0)).isEqualTo(0.0);
    }

    @Test
    void single_success_conservative_score() {
        double score = WilsonConfidence.score(1.0, 1);
        assertThat(score).isBetween(0.20, 0.35);
    }

    @Test
    void ten_successes_higher_confidence() {
        double score = WilsonConfidence.score(10.0, 10);
        assertThat(score).isGreaterThan(0.70);
    }

    @Test
    void all_failures_near_zero() {
        double score = WilsonConfidence.score(0.0, 10);
        assertThat(score).isLessThan(0.05);
    }

    @Test
    void mixed_outcomes_intermediate() {
        double score = WilsonConfidence.score(7.5, 10);
        assertThat(score).isBetween(0.45, 0.80);
    }
}

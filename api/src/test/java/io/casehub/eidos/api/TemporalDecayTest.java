package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class TemporalDecayTest {

    @Test
    void now_returns_one() {
        var now = Instant.now();
        assertThat(TemporalDecay.exponential(now, now, 90)).isEqualTo(1.0);
    }

    @Test
    void at_half_life_returns_half() {
        var now = Instant.now();
        var halfLifeAgo = now.minus(90, ChronoUnit.DAYS);
        assertThat(TemporalDecay.exponential(halfLifeAgo, now, 90)).isCloseTo(0.5, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    void at_two_half_lives_returns_quarter() {
        var now = Instant.now();
        var twoHalfLivesAgo = now.minus(180, ChronoUnit.DAYS);
        assertThat(TemporalDecay.exponential(twoHalfLivesAgo, now, 90)).isCloseTo(0.25, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    void recent_outcome_near_one() {
        var now = Instant.now();
        var oneDay = now.minus(1, ChronoUnit.DAYS);
        assertThat(TemporalDecay.exponential(oneDay, now, 90)).isGreaterThan(0.99);
    }

    @Test
    void very_old_outcome_near_zero() {
        var now = Instant.now();
        var veryOld = now.minus(365, ChronoUnit.DAYS);
        assertThat(TemporalDecay.exponential(veryOld, now, 90)).isLessThan(0.07);
    }

    @Test
    void zero_half_life_returns_one() {
        var now = Instant.now();
        var old = now.minus(100, ChronoUnit.DAYS);
        assertThat(TemporalDecay.exponential(old, now, 0)).isEqualTo(1.0);
    }

    @Test
    void negative_half_life_returns_one() {
        var now = Instant.now();
        var old = now.minus(100, ChronoUnit.DAYS);
        assertThat(TemporalDecay.exponential(old, now, -1)).isEqualTo(1.0);
    }

    @Test
    void future_observation_returns_one() {
        var now = Instant.now();
        var future = now.plus(10, ChronoUnit.DAYS);
        assertThat(TemporalDecay.exponential(future, now, 90)).isEqualTo(1.0);
    }
}

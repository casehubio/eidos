package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ValenceCountsTest {

    @Test
    void effectiveWithFullDampening() {
        var vc = new ValenceCounts(5, 3);
        assertThat(vc.effective(1.0)).isEqualTo(8);
    }

    @Test
    void effectiveWithHalfDampening() {
        var vc = new ValenceCounts(5, 3);
        assertThat(vc.effective(0.5)).isEqualTo(7);
    }

    @Test
    void effectiveWithZeroDampening() {
        var vc = new ValenceCounts(5, 3);
        assertThat(vc.effective(0.0)).isEqualTo(5);
    }

    @Test
    void effectiveWithZeroCounts() {
        var vc = new ValenceCounts(0, 0);
        assertThat(vc.effective(0.5)).isEqualTo(0);
    }

    @Test
    void effectiveWithOnlyNegative() {
        var vc = new ValenceCounts(0, 4);
        assertThat(vc.effective(0.5)).isEqualTo(2);
    }
}

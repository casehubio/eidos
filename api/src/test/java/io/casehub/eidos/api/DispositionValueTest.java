package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DispositionValueTest {

    @Test void valid_construction() {
        var dv = new DispositionValue("independent", 0.7);
        assertThat(dv.term()).isEqualTo("independent");
        assertThat(dv.weight()).isEqualTo(0.7);
    }

    @Test void of_factory_defaults_weight_to_one() {
        var dv = DispositionValue.of("independent");
        assertThat(dv.term()).isEqualTo("independent");
        assertThat(dv.weight()).isEqualTo(1.0);
    }

    @Test void null_term_throws() {
        assertThatThrownBy(() -> new DispositionValue(null, 0.5))
                .isInstanceOf(AgentValidationException.class);
    }

    @Test void blank_term_throws() {
        assertThatThrownBy(() -> new DispositionValue("  ", 0.5))
                .isInstanceOf(AgentValidationException.class);
    }

    @Test void term_with_bidi_throws() {
        assertThatThrownBy(() -> new DispositionValue("ti‮injection", 0.5))
                .isInstanceOf(AgentValidationException.class);
    }

    @Test void term_with_zero_width_throws() {
        assertThatThrownBy(() -> new DispositionValue("ti​x", 0.5))
                .isInstanceOf(AgentValidationException.class);
    }

    @Test void term_exceeding_200_chars_throws() {
        assertThatThrownBy(() -> new DispositionValue("x".repeat(201), 0.5))
                .isInstanceOf(AgentValidationException.class);
    }

    @Test void negative_weight_throws() {
        assertThatThrownBy(() -> new DispositionValue("x", -0.1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void weight_above_one_throws() {
        assertThatThrownBy(() -> new DispositionValue("x", 1.1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void nan_weight_throws() {
        assertThatThrownBy(() -> new DispositionValue("x", Double.NaN))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void zero_weight_is_valid() {
        assertThatNoException().isThrownBy(() -> new DispositionValue("x", 0.0));
    }

    @Test void weight_one_is_valid() {
        assertThatNoException().isThrownBy(() -> new DispositionValue("x", 1.0));
    }

    @Test void boundary_weight_valid() {
        assertThatNoException().isThrownBy(() -> new DispositionValue("x", 0.001));
        assertThatNoException().isThrownBy(() -> new DispositionValue("x", 0.999));
    }

    @Test void equality_based_on_term_and_weight() {
        var a = new DispositionValue("ti", 0.45);
        var b = new DispositionValue("ti", 0.45);
        assertThat(a).isEqualTo(b);
    }
}

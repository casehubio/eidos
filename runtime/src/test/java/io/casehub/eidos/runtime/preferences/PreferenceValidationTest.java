package io.casehub.eidos.runtime.preferences;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class PreferenceValidationTest {

    @Test
    void exclude_threshold_rejects_zero() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new ExcludeThresholdPreference(0))
            .withMessageContaining("must be >= 1");
    }

    @Test
    void exclude_threshold_rejects_negative() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new ExcludeThresholdPreference(-1))
            .withMessageContaining("must be >= 1");
    }

    @Test
    void exclude_threshold_accepts_minimum() {
        final var pref = new ExcludeThresholdPreference(1);
        assertThat(pref.value()).isEqualTo(1);
    }

    @Test
    void compliance_violation_threshold_rejects_zero() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new ComplianceViolationThresholdPreference(0))
            .withMessageContaining("must be >= 1");
    }

    @Test
    void compliance_violation_threshold_rejects_negative() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new ComplianceViolationThresholdPreference(-1))
            .withMessageContaining("must be >= 1");
    }

    @Test
    void compliance_violation_threshold_accepts_minimum() {
        final var pref = new ComplianceViolationThresholdPreference(1);
        assertThat(pref.value()).isEqualTo(1);
    }

    @Test
    void aggregate_violation_threshold_rejects_zero() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new AggregateViolationThresholdPreference(0))
            .withMessageContaining("must be >= 1");
    }

    @Test
    void aggregate_violation_threshold_rejects_negative() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new AggregateViolationThresholdPreference(-1))
            .withMessageContaining("must be >= 1");
    }

    @Test
    void aggregate_violation_threshold_accepts_minimum() {
        final var pref = new AggregateViolationThresholdPreference(1);
        assertThat(pref.value()).isEqualTo(1);
    }
}

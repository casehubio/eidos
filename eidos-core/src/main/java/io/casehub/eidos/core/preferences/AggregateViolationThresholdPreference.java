package io.casehub.eidos.core.preferences;

import io.casehub.platform.api.preferences.SingleValuePreference;

public record AggregateViolationThresholdPreference(int value) implements SingleValuePreference {
    public AggregateViolationThresholdPreference {
        if (value < 1) throw new IllegalArgumentException(
            "behavioral.aggregate-violation-threshold must be >= 1, got: " + value);
    }
    @Override public String toSerializedValue() { return String.valueOf(value); }
}

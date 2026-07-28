package io.casehub.eidos.runtime.preferences;

import io.casehub.platform.api.preferences.SingleValuePreference;

public record OverReinforcementThresholdPreference(double value) implements SingleValuePreference {
    public OverReinforcementThresholdPreference {
        if (Double.isNaN(value) || value < 0.0 || value > 1.0)
            throw new IllegalArgumentException(
                    "disposition.over-reinforcement-threshold must be 0.0–1.0, got: " + value);
    }
    @Override public String toSerializedValue() { return String.valueOf(value); }
}

package io.casehub.eidos.runtime.preferences;

import io.casehub.platform.api.preferences.SingleValuePreference;

public record ReinforcementDeltaPreference(double value) implements SingleValuePreference {
    public ReinforcementDeltaPreference {
        if (Double.isNaN(value) || value < 0.0 || value > 1.0)
            throw new IllegalArgumentException(
                    "disposition.reinforcement-delta must be 0.0–1.0, got: " + value);
    }
    @Override public String toSerializedValue() { return String.valueOf(value); }
}

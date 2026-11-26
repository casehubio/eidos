package io.casehub.eidos.core.preferences;

import io.casehub.platform.api.preferences.SingleValuePreference;

public record DetectionConfidencePreference(double value) implements SingleValuePreference {
    public DetectionConfidencePreference {
        if (value < 0.0 || value > 1.0) throw new IllegalArgumentException(
            "stigmergy.detection-confidence-threshold must be in [0.0, 1.0], got: " + value);
    }
    @Override public String toSerializedValue() { return String.valueOf(value); }
}

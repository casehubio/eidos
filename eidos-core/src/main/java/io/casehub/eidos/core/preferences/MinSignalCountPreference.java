package io.casehub.eidos.core.preferences;

import io.casehub.platform.api.preferences.SingleValuePreference;

public record MinSignalCountPreference(int value) implements SingleValuePreference {
    public MinSignalCountPreference {
        if (value < 1) throw new IllegalArgumentException(
            "stigmergy.min-signal-count must be >= 1, got: " + value);
    }
    @Override public String toSerializedValue() { return String.valueOf(value); }
}

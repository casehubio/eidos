package io.casehub.eidos.runtime.preferences;

import io.casehub.platform.api.preferences.SingleValuePreference;

public record ExcludeThresholdPreference(int value) implements SingleValuePreference {
    public ExcludeThresholdPreference {
        if (value < 1) throw new IllegalArgumentException(
            "specialization.exclude-threshold must be >= 1, got: " + value);
    }
}

package io.casehub.eidos.core.preferences;

import io.casehub.platform.api.preferences.SingleValuePreference;

public record SwapMarginPreference(double value) implements SingleValuePreference {
    public SwapMarginPreference {
        if (Double.isNaN(value) || value < 0.0 || value > 0.5)
            throw new IllegalArgumentException(
                    "disposition.swap-margin must be 0.0–0.5, got: " + value);
    }
    @Override public String toSerializedValue() { return String.valueOf(value); }
}

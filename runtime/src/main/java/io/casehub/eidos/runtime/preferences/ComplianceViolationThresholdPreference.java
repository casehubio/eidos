package io.casehub.eidos.runtime.preferences;

import io.casehub.platform.api.preferences.SingleValuePreference;

public record ComplianceViolationThresholdPreference(int value) implements SingleValuePreference {
    public ComplianceViolationThresholdPreference {
        if (value < 1) throw new IllegalArgumentException(
            "behavioral.compliance-violation-threshold must be >= 1, got: " + value);
    }
    @Override public String toSerializedValue() { return String.valueOf(value); }
}

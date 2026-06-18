package io.casehub.eidos.runtime.preferences;

import io.casehub.platform.api.preferences.SingleValuePreference;

public record ExcludeThresholdPreference(int value) implements SingleValuePreference {}

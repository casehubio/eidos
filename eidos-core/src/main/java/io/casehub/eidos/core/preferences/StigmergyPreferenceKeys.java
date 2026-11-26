package io.casehub.eidos.core.preferences;

import io.casehub.platform.api.preferences.PreferenceKey;

public final class StigmergyPreferenceKeys {

    private StigmergyPreferenceKeys() {}

    public static final PreferenceKey<DetectionConfidencePreference> DETECTION_CONFIDENCE_THRESHOLD =
        new PreferenceKey<>("casehub.eidos", "stigmergy.detection-confidence-threshold",
                            new DetectionConfidencePreference(0.6),
                            s -> new DetectionConfidencePreference(Double.parseDouble(s)));

    public static final PreferenceKey<MinSignalCountPreference> MIN_SIGNAL_COUNT =
        new PreferenceKey<>("casehub.eidos", "stigmergy.min-signal-count",
                            new MinSignalCountPreference(5),
                            s -> new MinSignalCountPreference(Integer.parseInt(s)));
}

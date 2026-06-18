package io.casehub.eidos.runtime.preferences;

import io.casehub.platform.api.preferences.PreferenceKey;

public final class EidosPreferenceKeys {

    private EidosPreferenceKeys() {}

    /**
     * Minimum number of unexpired DECLINE records for a domain before eidos treats it as a
     * learned exclusion. Resolved per-tenancy via PreferenceProvider ancestor-chain walk.
     * Default: 3. Revisit once casehub-ledger/CBR integration produces real DECLINE data.
     */
    public static final PreferenceKey<ExcludeThresholdPreference> EXCLUDE_THRESHOLD =
        new PreferenceKey<>("casehub.eidos", "specialization.exclude-threshold",
                            new ExcludeThresholdPreference(3),
                            s -> new ExcludeThresholdPreference(Integer.parseInt(s)));
}

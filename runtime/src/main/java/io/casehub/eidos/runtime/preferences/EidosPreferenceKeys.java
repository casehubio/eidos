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

    /**
     * Minimum number of unexpired VIOLATED records for a compliance dimension before
     * eidos reports it as a behavioral violation. Resolved per-tenancy via
     * PreferenceProvider ancestor-chain walk.
     * Default: 3.
     */
    public static final PreferenceKey<ComplianceViolationThresholdPreference> COMPLIANCE_VIOLATION_THRESHOLD =
        new PreferenceKey<>("casehub.eidos", "behavioral.compliance-violation-threshold",
                            new ComplianceViolationThresholdPreference(3),
                            s -> new ComplianceViolationThresholdPreference(Integer.parseInt(s)));

    /**
     * Minimum total count of unexpired VIOLATED records across all compliance
     * dimensions before eidos reports an aggregate behavioral violation.
     * Only fires when no individual dimension exceeds the per-dimension threshold.
     * Default: 5.
     */
    public static final PreferenceKey<AggregateViolationThresholdPreference> AGGREGATE_VIOLATION_THRESHOLD =
        new PreferenceKey<>("casehub.eidos", "behavioral.aggregate-violation-threshold",
                            new AggregateViolationThresholdPreference(5),
                            s -> new AggregateViolationThresholdPreference(Integer.parseInt(s)));
}

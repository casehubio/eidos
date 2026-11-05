package io.casehub.eidos.org.api;

public record GroupPolicy(
        FormationRules formationRules,
        int normativePriority,
        NormSpecificity normSpecificity,
        long dissolutionDrainTimeoutMinutes
) {
    public static final long DEFAULT_DISSOLUTION_DRAIN_TIMEOUT_MINUTES = 30;

    public GroupPolicy {
        if (normativePriority < 0) {
            throw new OrgValidationException("normativePriority", "must be >= 0 (was " + normativePriority + ")");
        }
        if (dissolutionDrainTimeoutMinutes == 0) {
            dissolutionDrainTimeoutMinutes = DEFAULT_DISSOLUTION_DRAIN_TIMEOUT_MINUTES;
        }
        if (dissolutionDrainTimeoutMinutes < 0) {
            throw new OrgValidationException("dissolutionDrainTimeoutMinutes", "must be > 0 (was " + dissolutionDrainTimeoutMinutes + ")");
        }
    }
}

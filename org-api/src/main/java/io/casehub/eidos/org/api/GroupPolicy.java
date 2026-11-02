package io.casehub.eidos.org.api;

public record GroupPolicy(
    FormationRules formationRules,
    int normativePriority,
    NormSpecificity normSpecificity
) {
    public GroupPolicy {
        if (normativePriority < 0) throw new OrgValidationException("normativePriority", "must be >= 0 (was " + normativePriority + ")");
    }
}

package io.casehub.eidos.org.api;

import java.util.Set;

public record FormationRules(
    Set<FormationPath> allowedPaths,
    int minGroupSize,
    int maxGroupSize,
    boolean allowSubGroups,
    double proposalQuorum
) {
    public static final int DEFAULT_MIN_GROUP_SIZE = 2;
    public static final double DEFAULT_QUORUM = 1.0;

    public FormationRules {
        allowedPaths = allowedPaths != null ? Set.copyOf(allowedPaths) : Set.of(FormationPath.values());
        if (minGroupSize < 1) throw new OrgValidationException("minGroupSize", "must be >= 1 (was " + minGroupSize + ")");
        if (maxGroupSize != -1 && maxGroupSize < minGroupSize)
            throw new OrgValidationException("maxGroupSize", "must be >= minGroupSize or -1 (was " + maxGroupSize + ", min " + minGroupSize + ")");
        if (proposalQuorum < 0.0 || proposalQuorum > 1.0)
            throw new OrgValidationException("proposalQuorum", "must be in [0.0, 1.0] (was " + proposalQuorum + ")");
    }
}

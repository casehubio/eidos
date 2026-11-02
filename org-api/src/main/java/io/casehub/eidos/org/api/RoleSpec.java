package io.casehub.eidos.org.api;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record RoleSpec(
    String roleName,
    String roleVocabulary,
    int minCardinality,
    int maxCardinality,
    Set<String> incompatibleWith,
    List<RolePolicy> policies
) {
    public RoleSpec {
        Objects.requireNonNull(roleName, "roleName");
        if (roleName.isBlank()) throw new OrgValidationException("roleName", "must not be blank");
        if (minCardinality < 0) throw new OrgValidationException("minCardinality", "must be >= 0 (was " + minCardinality + ")");
        if (maxCardinality != -1 && maxCardinality < minCardinality)
            throw new OrgValidationException("maxCardinality", "must be >= minCardinality or -1 (was " + maxCardinality + ", min " + minCardinality + ")");
        incompatibleWith = incompatibleWith != null ? Set.copyOf(incompatibleWith) : Set.of();
        policies = policies != null ? List.copyOf(policies) : List.of();
    }
}

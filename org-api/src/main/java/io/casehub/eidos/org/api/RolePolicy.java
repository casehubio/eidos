package io.casehub.eidos.org.api;

import java.util.Map;
import java.util.Objects;

public record RolePolicy(
    String capabilityName,
    PolicyKind kind,
    Map<String, String> expectations
) {
    public RolePolicy {
        Objects.requireNonNull(capabilityName, "capabilityName");
        if (capabilityName.isBlank()) throw new OrgValidationException("capabilityName", "must not be blank");
        Objects.requireNonNull(kind, "kind");
        expectations = expectations != null ? Map.copyOf(expectations) : Map.of();
    }
}

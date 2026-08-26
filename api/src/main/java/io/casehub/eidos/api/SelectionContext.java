package io.casehub.eidos.api;

import java.util.Objects;

public record SelectionContext(
    String tenancyId,
    String capabilityName,
    String taskDomain
) {
    public SelectionContext {
        Objects.requireNonNull(tenancyId, "tenancyId");
    }

    public static SelectionContext of(String tenancyId, String capabilityName) {
        return new SelectionContext(tenancyId, capabilityName, null);
    }

    public static SelectionContext of(String tenancyId, String capabilityName, String taskDomain) {
        return new SelectionContext(tenancyId, capabilityName, taskDomain);
    }
}

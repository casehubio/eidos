package io.casehub.eidos.org.api;

import java.util.Objects;

public record OrgQuery(
    String kind,
    String tenancyId,
    String parentUnitId
) {
    public OrgQuery {
        Objects.requireNonNull(tenancyId, "tenancyId");
    }

    public static OrgQuery byKind(String kind, String tenancyId) {
        return new OrgQuery(kind, tenancyId, null);
    }

    public static OrgQuery byParent(String parentUnitId, String tenancyId) {
        return new OrgQuery(null, tenancyId, parentUnitId);
    }

    public static OrgQuery all(String tenancyId) {
        return new OrgQuery(null, tenancyId, null);
    }
}

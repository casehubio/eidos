package io.casehub.eidos.org.runtime.yaml;

import io.casehub.eidos.org.api.AgentRelationship;
import io.casehub.eidos.org.api.OrganizationalUnit;
import io.casehub.eidos.org.api.spi.OrgRegistrar;

import java.util.List;

public record YamlOrganization(
    List<OrganizationalUnit> units,
    List<AgentRelationship> relationships
) {
    public YamlOrganization {
        units = units != null ? List.copyOf(units) : List.of();
        relationships = relationships != null ? List.copyOf(relationships) : List.of();
    }

    public OrgRegistrar.OrgDefinition toOrgDefinition() {
        return new OrgRegistrar.OrgDefinition(units, relationships);
    }
}

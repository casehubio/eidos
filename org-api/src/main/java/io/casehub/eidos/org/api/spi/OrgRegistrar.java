package io.casehub.eidos.org.api.spi;

import io.casehub.eidos.org.api.AgentRelationship;
import io.casehub.eidos.org.api.OrganizationalUnit;

import java.util.List;

@FunctionalInterface
public interface OrgRegistrar {
    OrgDefinition organization();

    record OrgDefinition(
        List<OrganizationalUnit> units,
        List<AgentRelationship> relationships
    ) {
        public OrgDefinition {
            units = units != null ? List.copyOf(units) : List.of();
            relationships = relationships != null ? List.copyOf(relationships) : List.of();
        }
    }
}

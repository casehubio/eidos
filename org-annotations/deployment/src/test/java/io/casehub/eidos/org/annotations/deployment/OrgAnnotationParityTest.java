package io.casehub.eidos.org.annotations.deployment;

import io.casehub.eidos.org.annotations.OrgMemberDef;
import io.casehub.eidos.org.annotations.OrgRelationshipDef;
import io.casehub.eidos.org.annotations.OrgUnit;
import io.casehub.eidos.org.annotations.Supervises;
import io.casehub.eidos.org.api.AgentRelationship;
import io.casehub.eidos.org.api.Membership;
import io.casehub.eidos.org.api.OrganizationalUnit;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class OrgAnnotationParityTest {

    private static final Map<String, String> ORG_UNIT_RENAMES = Map.of(
            "id", "unitId",
            "parentUnit", "parentUnitId"
    );

    private static final Set<String> ORG_UNIT_INFRA_FIELDS = Set.of(
            "tenancyId", "members"
    );

    private static final Set<String> RELATIONSHIP_INFRA_FIELDS = Set.of(
            "tenancyId"
    );

    @Test
    void everyOrgUnitFieldHasBuilderSetter() {
        var annotationFields = Arrays.stream(OrgUnit.class.getDeclaredMethods())
                .map(m -> m.getName()).collect(Collectors.toSet());
        var builderSetters = Arrays.stream(OrganizationalUnit.Builder.class.getDeclaredMethods())
                .map(m -> m.getName()).collect(Collectors.toSet());
        for (var field : annotationFields) {
            var builderName = ORG_UNIT_RENAMES.getOrDefault(field, field);
            assertThat(builderSetters).as("Builder setter for @OrgUnit.%s() (→ %s)", field, builderName)
                    .contains(builderName);
        }
    }

    @Test
    void everyOrgUnitBuilderSetterIsReachable() {
        var builderSetters = Arrays.stream(OrganizationalUnit.Builder.class.getDeclaredMethods())
                .map(m -> m.getName())
                .filter(m -> !m.equals("build"))
                .collect(Collectors.toSet());
        var annotationFields = Arrays.stream(OrgUnit.class.getDeclaredMethods())
                .map(m -> ORG_UNIT_RENAMES.getOrDefault(m.getName(), m.getName()))
                .collect(Collectors.toSet());
        for (var setter : builderSetters) {
            if (ORG_UNIT_INFRA_FIELDS.contains(setter)) continue;
            assertThat(annotationFields).as("@OrgUnit field for OrganizationalUnit.Builder.%s()", setter)
                    .contains(setter);
        }
    }

    @Test
    void everyMemberDefFieldHasRecordComponent() {
        var annotationFields = Arrays.stream(OrgMemberDef.class.getDeclaredMethods())
                .map(m -> m.getName()).collect(Collectors.toSet());
        var recordComponents = Arrays.stream(Membership.class.getRecordComponents())
                .map(c -> c.getName()).collect(Collectors.toSet());
        for (var field : annotationFields) {
            assertThat(recordComponents).as("Membership component for @OrgMemberDef.%s()", field)
                    .contains(field);
        }
    }

    @Test
    void everyMembershipComponentIsReachableFromAnnotation() {
        var recordComponents = Arrays.stream(Membership.class.getRecordComponents())
                .map(c -> c.getName()).collect(Collectors.toSet());
        var annotationFields = Arrays.stream(OrgMemberDef.class.getDeclaredMethods())
                .map(m -> m.getName()).collect(Collectors.toSet());
        for (var component : recordComponents) {
            assertThat(annotationFields).as("@OrgMemberDef field for Membership.%s()", component)
                    .contains(component);
        }
    }

    private static final Map<String, String> RELATIONSHIP_RENAMES = Map.of(
            "source", "sourceAgentId",
            "target", "targetAgentId"
    );

    private static final Set<String> RELATIONSHIP_COMPOSITE_FIELDS = Set.of(
            "scopeDomain", "scopeCondition"
    );

    @Test
    void everyRelationshipDefFieldHasBuilderSetter() {
        var annotationFields = Arrays.stream(OrgRelationshipDef.class.getDeclaredMethods())
                .map(m -> m.getName()).collect(Collectors.toSet());
        var builderSetters = Arrays.stream(AgentRelationship.Builder.class.getDeclaredMethods())
                .map(m -> m.getName()).collect(Collectors.toSet());
        for (var field : annotationFields) {
            if (RELATIONSHIP_COMPOSITE_FIELDS.contains(field)) continue;
            var builderName = RELATIONSHIP_RENAMES.getOrDefault(field, field);
            assertThat(builderSetters).as("Builder setter for @OrgRelationshipDef.%s() (→ %s)", field, builderName)
                    .contains(builderName);
        }
    }

    @Test
    void everyRelationshipBuilderSetterIsReachable() {
        var builderSetters = Arrays.stream(AgentRelationship.Builder.class.getDeclaredMethods())
                .map(m -> m.getName())
                .filter(m -> !m.equals("build"))
                .collect(Collectors.toSet());
        var annotationFields = Arrays.stream(OrgRelationshipDef.class.getDeclaredMethods())
                .map(m -> m.getName()).collect(Collectors.toSet());
        var supervisesFields = Arrays.stream(Supervises.class.getDeclaredMethods())
                .map(m -> m.getName()).collect(Collectors.toSet());
        var allAnnotationFields = new java.util.HashSet<>(annotationFields);
        allAnnotationFields.addAll(supervisesFields);
        for (var setter : builderSetters) {
            if (RELATIONSHIP_INFRA_FIELDS.contains(setter)) continue;
            if (setter.equals("sourceAgentId")) {
                assertThat(allAnnotationFields).as("Annotation field for AgentRelationship.Builder.%s()", setter)
                        .contains("source");
            } else if (setter.equals("targetAgentId")) {
                assertThat(allAnnotationFields).as("Annotation field for AgentRelationship.Builder.%s()", setter)
                        .contains("target");
            } else {
                assertThat(allAnnotationFields).as("Annotation field for AgentRelationship.Builder.%s()", setter)
                        .contains(setter);
            }
        }
    }
}

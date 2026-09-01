package io.casehub.eidos.org.examples;

import io.casehub.eidos.org.api.RelationshipKind;
import io.casehub.eidos.org.memory.InMemoryOrgRegistry;
import io.casehub.eidos.org.runtime.yaml.ClasspathYamlOrgRegistrar;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * YAML-driven Gastown organization — same topology as GastownOrgExample
 * but loaded from META-INF/eidos/organization.yaml.
 *
 * Demonstrates: YAML parity with DSL, classpath loading, YAML
 * as the source of truth for diagram editors.
 */
class YamlOrgTest {

    @Test void loadGastownFromYaml() {
        var registrar = new ClasspathYamlOrgRegistrar();
        var yaml = getClass().getResourceAsStream("/META-INF/eidos/organization.yaml");
        var org = registrar.loadFrom(yaml);

        // Same structure as DSL example
        assertThat(org.units()).hasSize(3);
        assertThat(org.relationships()).hasSize(10);

        // Register and query
        var registry = new InMemoryOrgRegistry();
        org.units().forEach(registry::registerUnit);
        org.relationships().forEach(registry::addRelationship);

        // 4-level supervision chain
        assertThat(registry.supervisors("polecat-1", "gastown").getFirst().sourceAgentId())
            .isEqualTo("witness-alpha");
        assertThat(registry.supervisors("witness-alpha", "gastown").getFirst().sourceAgentId())
            .isEqualTo("deacon");

        // Two rigs
        assertThat(registry.findUnits(
            io.casehub.eidos.org.api.OrgQuery.byKind("rig", "gastown"))).hasSize(2);

        // Escalation path
        var path = registry.escalationPath("polecat-1", "gastown");
        assertThat(path).extracting(r -> r.targetAgentId())
            .containsExactly("witness-alpha", "deacon", "boot");

        // Scoped backup
        var backup = registry.relationshipsFrom("polecat-1", "gastown").stream()
            .filter(r -> r.kind() == RelationshipKind.BACKS_UP).findFirst().orElseThrow();
        assertThat(backup.scope().capabilityName()).isEqualTo("code-analysis");
    }
}

package io.casehub.eidos.org.annotations.deployment;

import io.casehub.eidos.org.api.OrgRegistry;
import io.casehub.eidos.org.api.RelationshipKind;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class EidosOrgAnnotationsProcessorTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClass(io.casehub.eidos.org.annotations.deployment.test.SimpleOrgUnit.class)
                    .addClass(io.casehub.eidos.org.annotations.deployment.test.ExplicitIdOrgUnit.class)
                    .addClass(io.casehub.eidos.org.annotations.deployment.test.MinimalOrgUnit.class)
                    .addClass(io.casehub.eidos.org.annotations.deployment.test.HierarchyChildUnit.class))
            .overrideConfigKey("casehub.eidos.annotations.default-tenancy-id", "test-tenant")
            .overrideConfigKey("casehub.eidos.reactive.enabled", "false")
            .overrideConfigKey("quarkus.datasource.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.jdbc.url", "jdbc:h2:mem:organntest;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
            .overrideConfigKey("quarkus.datasource.devservices.enabled", "false")
            .overrideConfigKey("quarkus.flyway.migrate-at-start", "false")
            .overrideConfigKey("quarkus.hibernate-orm.database.generation", "none");

    @Inject
    OrgRegistry registry;

    @Test
    void simpleOrgUnitIsRegistered() {
        var result = registry.findUnit("simple-org-unit", "test-tenant");
        assertThat(result).isPresent();
        var unit = result.get();
        assertThat(unit.kind()).isEqualTo("rig");
        assertThat(unit.name()).isEqualTo("Simple Org Unit");
    }

    @Test
    void simpleOrgUnitHasMembers() {
        var unit = registry.findUnit("simple-org-unit", "test-tenant").orElseThrow();
        assertThat(unit.members()).hasSize(3);
        assertThat(unit.hasMember("witness-1")).isTrue();
        assertThat(unit.hasMember("polecat-1")).isTrue();
        assertThat(unit.hasMember("polecat-2")).isTrue();
        var witness = unit.members().stream()
                .filter(m -> m.agentId().equals("witness-1")).findFirst().orElseThrow();
        assertThat(witness.role()).isEqualTo("witness");
    }

    @Test
    void simpleOrgUnitHasSupervisionRelationships() {
        var subs = registry.subordinates("witness-1", "test-tenant");
        assertThat(subs).hasSize(2);
        assertThat(subs).extracting(r -> r.targetAgentId())
                .containsExactlyInAnyOrder("polecat-1", "polecat-2");
    }

    @Test
    void explicitIdOrgUnitIsRegistered() {
        var result = registry.findUnit("oversight", "test-tenant");
        assertThat(result).isPresent();
        var unit = result.get();
        assertThat(unit.name()).isEqualTo("Oversight Chain");
        assertThat(unit.kind()).isEqualTo("supervision-hierarchy");
    }

    @Test
    void explicitIdOrgUnitHasGenericRelationships() {
        var subs = registry.subordinates("boot", "test-tenant");
        assertThat(subs).hasSize(1);
        assertThat(subs.getFirst().targetAgentId()).isEqualTo("deacon");

        var backups = registry.relationshipsFrom("polecat-1", "test-tenant").stream()
                .filter(r -> r.kind() == RelationshipKind.BACKS_UP).toList();
        assertThat(backups).hasSize(1);
        assertThat(backups.getFirst().scope().capabilityName()).isEqualTo("code-analysis");
    }

    @Test
    void minimalOrgUnitHasNoMembersOrRelationships() {
        var unit = registry.findUnit("minimal-org-unit", "test-tenant").orElseThrow();
        assertThat(unit.kind()).isEqualTo("team");
        assertThat(unit.members()).isEmpty();
    }

    @Test
    void hierarchyChildUnitHasParent() {
        var unit = registry.findUnit("sub-team", "test-tenant").orElseThrow();
        assertThat(unit.parentUnitId()).isEqualTo("minimal-org-unit");
        assertThat(unit.kind()).isEqualTo("squad");
        assertThat(unit.members()).hasSize(1);
        assertThat(unit.members().getFirst().agentId()).isEqualTo("agent-1");
        assertThat(unit.members().getFirst().role()).isNull();
    }

    @Test
    void tenancyIdComesFromConfig() {
        var unit = registry.findUnit("oversight", "test-tenant").orElseThrow();
        assertThat(unit.tenancyId()).isEqualTo("test-tenant");
    }

    @Test
    void ancestorTraversal() {
        var ancestors = registry.ancestorUnits("sub-team", "test-tenant");
        assertThat(ancestors).hasSize(1);
        assertThat(ancestors.getFirst().unitId()).isEqualTo("minimal-org-unit");
    }
}

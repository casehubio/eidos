package io.casehub.eidos.annotations.deployment;

import io.casehub.eidos.api.AgentRegistry;
import io.casehub.eidos.api.ConstraintSeverity;
import io.casehub.eidos.api.DispositionAxis;
import io.casehub.eidos.api.GoalPriority;
import io.casehub.eidos.api.Visibility;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class EidosAnnotationsProcessorTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClass(io.casehub.eidos.annotations.deployment.test.SimpleAnnotatedAgent.class)
                    .addClass(io.casehub.eidos.annotations.deployment.test.FullAnnotatedAgent.class)
                    .addClass(io.casehub.eidos.annotations.deployment.test.IdentityOnlyAgent.class)
                    .addClass(io.casehub.eidos.annotations.deployment.test.WeightedDispositionAgent.class)
                    .addClass(io.casehub.eidos.annotations.deployment.test.RichCapabilityAgent.class)
                    .addClass(io.casehub.eidos.annotations.deployment.test.CapabilityMergeAgent.class)
                    .addClass(io.casehub.eidos.annotations.deployment.test.TemplatedAgent.class)
                    .addClass(io.casehub.eidos.annotations.deployment.test.TestTemplateRegistrar.class))
            .overrideConfigKey("casehub.eidos.annotations.default-tenancy-id", "test-tenant")
            .overrideConfigKey("casehub.eidos.reactive.enabled", "false")
            .overrideConfigKey("quarkus.datasource.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.jdbc.url", "jdbc:h2:mem:anntest;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
            .overrideConfigKey("quarkus.datasource.devservices.enabled", "false")
            .overrideConfigKey("quarkus.flyway.migrate-at-start", "false")
            .overrideConfigKey("quarkus.hibernate-orm.database.generation", "none");

    @Inject
    AgentRegistry registry;

    @Test
    void simpleAnnotatedAgentIsRegistered() {
        var result = registry.findById("simple-annotated-agent", "test-tenant");
        assertThat(result).isPresent();
        var d = result.get();
        assertThat(d.slot()).isEqualTo("test-agent");
        assertThat(d.briefing()).isEqualTo("A test agent");
        assertThat(d.name()).isEqualTo("Simple Annotated Agent");
    }

    @Test
    void simpleAnnotatedAgentHasDisposition() {
        var d = registry.findById("simple-annotated-agent", "test-tenant").orElseThrow();
        assertThat(d.disposition()).isNotNull();
        assertThat(d.disposition().primaryTerm(DispositionAxis.SOCIAL_ORIENTATION))
            .isEqualTo("collaborative");
        assertThat(d.disposition().primaryTerm(DispositionAxis.RULE_FOLLOWING))
            .isEqualTo("strict");
    }

    @Test
    void fullAnnotatedAgentHasExplicitIdAndName() {
        var d = registry.findById("full-agent", "test-tenant").orElseThrow();
        assertThat(d.agentId()).isEqualTo("full-agent");
        assertThat(d.name()).isEqualTo("Full Agent");
        assertThat(d.slot()).isEqualTo("analyst");
        assertThat(d.jurisdiction()).isEqualTo("EU");
    }

    @Test
    void fullAnnotatedAgentHasCapabilities() {
        var d = registry.findById("full-agent", "test-tenant").orElseThrow();
        assertThat(d.capabilities()).hasSize(2);
        assertThat(d.capabilities().stream().map(c -> c.name()).toList())
            .containsExactly("analysis", "review");
    }

    @Test
    void fullAnnotatedAgentHasGoals() {
        var d = registry.findById("full-agent", "test-tenant").orElseThrow();
        assertThat(d.goals()).hasSize(2);
        var primary = d.goals().stream().filter(g -> g.name().equals("accurate")).findFirst().orElseThrow();
        assertThat(primary.priority()).isEqualTo(GoalPriority.PRIMARY);
        assertThat(primary.description()).isEqualTo("Be accurate");
        assertThat(primary.capabilities()).containsExactly("analysis");
    }

    @Test
    void fullAnnotatedAgentHasConstraints() {
        var d = registry.findById("full-agent", "test-tenant").orElseThrow();
        assertThat(d.constraints()).hasSize(2);
        var hard = d.constraints().stream().filter(c -> c.name().equals("no-advice")).findFirst().orElseThrow();
        assertThat(hard.severity()).isEqualTo(ConstraintSeverity.HARD);
        assertThat(hard.visibility()).isEqualTo(Visibility.PUBLIC);
        var soft = d.constraints().stream().filter(c -> c.name().equals("cite-sources")).findFirst().orElseThrow();
        assertThat(soft.visibility()).isEqualTo(Visibility.PRIVATE);
    }

    @Test
    void tenancyIdComesFromConfig() {
        var d = registry.findById("full-agent", "test-tenant").orElseThrow();
        assertThat(d.tenancyId()).isEqualTo("test-tenant");
    }

    @Test
    void identityOnlyAgentIsRegisteredWithNullDisposition() {
        var d = registry.findById("identity-only-agent", "test-tenant").orElseThrow();
        assertThat(d.slot()).isEqualTo("identity-only-agent");
        assertThat(d.briefing()).isEqualTo("No disposition, no goals, no constraints");
        assertThat(d.disposition()).isNull();
        assertThat(d.goals()).isEmpty();
        assertThat(d.constraints()).isEmpty();
        assertThat(d.capabilities()).isEmpty();
    }

    @Test
    void identityFieldsWeightsFingerprintAndModelVersion() {
        var d = registry.findById("weighted-disposition-agent", "test-tenant").orElseThrow();
        assertThat(d.weightsFingerprint()).isEqualTo("sha256:abc123");
        assertThat(d.modelVersion()).isEqualTo("2024-Q3");
    }

    @Test
    void weightedDispositionProfile() {
        var d = registry.findById("weighted-disposition-agent", "test-tenant").orElseThrow();
        assertThat(d.disposition().dispositionProfile()).hasSize(2);
        assertThat(d.disposition().dispositionProfile().get(0).term()).isEqualTo("collaborative");
        assertThat(d.disposition().dispositionProfile().get(0).weight()).isEqualTo(0.8);
        assertThat(d.disposition().dispositionProfile().get(1).term()).isEqualTo("analytical");
        assertThat(d.disposition().dispositionProfile().get(1).weight()).isEqualTo(0.4);
    }

    @Test
    void weightedStyleProfile() {
        var d = registry.findById("weighted-disposition-agent", "test-tenant").orElseThrow();
        assertThat(d.disposition().styleProfile()).hasSize(1);
        assertThat(d.disposition().styleProfile().get(0).term()).isEqualTo("concise");
        assertThat(d.disposition().styleProfile().get(0).weight()).isEqualTo(0.7);
    }

    @Test
    void axisVocabularies() {
        var d = registry.findById("weighted-disposition-agent", "test-tenant").orElseThrow();
        assertThat(d.axisVocabularies()).containsEntry(
                DispositionAxis.CONFLICT_MODE,
                "urn:casehub:vocab:thomas-kilmann");
    }

    @Test
    void richCapability_fullMetadata() {
        var d = registry.findById("rich-capability-agent", "test-tenant").orElseThrow();
        assertThat(d.capabilities()).hasSize(2);
        var analysis = d.capabilities().stream().filter(c -> c.name().equals("analysis")).findFirst().orElseThrow();
        assertThat(analysis.description()).isEqualTo("Deep analysis capability");
        assertThat(analysis.qualityHint()).isEqualTo(0.95);
        assertThat(analysis.latencyHintP50Ms()).isEqualTo(3000L);
        assertThat(analysis.costHint()).isEqualTo("medium");
        assertThat(analysis.inputTypes()).containsExactly("application/pdf", "text/plain");
        assertThat(analysis.outputTypes()).containsExactly("application/json");
        assertThat(analysis.tags()).containsExactly("nlp", "extraction");
        assertThat(analysis.epistemicDomains()).containsEntry("legal", 0.95);
        assertThat(analysis.epistemicDomains()).containsEntry("financial", 0.6);
        assertThat(analysis.excludedDomains()).containsExactly("criminal-law");
    }

    @Test
    void capabilityMerge_unionOfDiscoverableAndRichCaps() {
        var d = registry.findById("capability-merge-agent", "test-tenant").orElseThrow();
        assertThat(d.capabilities()).hasSize(2);
        assertThat(d.capabilities().stream().map(c -> c.name()).toList())
                .containsExactlyInAnyOrder("simple-cap", "rich-cap");
        var richCap = d.capabilities().stream().filter(c -> c.name().equals("rich-cap")).findFirst().orElseThrow();
        assertThat(richCap.qualityHint()).isEqualTo(0.9);
    }

    @Test
    void templates_repeatable() {
        var d = registry.findById("templated-agent", "test-tenant").orElseThrow();
        assertThat(d.templates()).hasSize(2);
        assertThat(d.templates().get(0).templateId()).isEqualTo("safety-primer");
        assertThat(d.templates().get(0).args()).containsEntry("domain", "legal");
        assertThat(d.templates().get(1).templateId()).isEqualTo("jurisdiction-notice");
        assertThat(d.templates().get(1).args()).containsEntry("region", "EU");
    }


}

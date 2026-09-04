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
                    .addClass(io.casehub.eidos.annotations.deployment.test.TestTemplateRegistrar.class)
                    .addClass(io.casehub.eidos.annotations.deployment.test.FullParityAgent.class)
                    .addClass(io.casehub.eidos.annotations.deployment.test.RepeatableGoalConstraintAgent.class))
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

    @Test
    void fullParity_annotationMatchesBuilder() {
        var annotated = registry.findById("parity-agent", "test-tenant").orElseThrow();

        assertThat(annotated.agentId()).isEqualTo("parity-agent");
        assertThat(annotated.name()).isEqualTo("Parity Agent");
        assertThat(annotated.slot()).isEqualTo("analyst");
        assertThat(annotated.tenancyId()).isEqualTo("test-tenant");
        assertThat(annotated.domainVocabulary()).isEqualTo("urn:casehub:vocab:svo");
        assertThat(annotated.dispositionVocabulary()).isEqualTo("urn:casehub:vocab:conscientiousness");
        assertThat(annotated.provider()).isEqualTo("test-provider");
        assertThat(annotated.modelFamily()).isEqualTo("test-model");
        assertThat(annotated.modelVersion()).isEqualTo("v1");
        assertThat(annotated.weightsFingerprint()).isEqualTo("sha256:parity");
        assertThat(annotated.jurisdiction()).isEqualTo("EU");
        assertThat(annotated.dataHandlingPolicy()).isEqualTo("gdpr");
        assertThat(annotated.briefing()).isEqualTo("Full parity test agent");
        assertThat(annotated.version()).isEqualTo("1.0");

        assertThat(annotated.disposition()).isNotNull();
        assertThat(annotated.disposition().primaryTerm(DispositionAxis.SOCIAL_ORIENTATION)).isEqualTo("collaborative");
        assertThat(annotated.disposition().primaryTerm(DispositionAxis.RULE_FOLLOWING)).isEqualTo("strict");
        assertThat(annotated.disposition().primaryTerm(DispositionAxis.RISK_APPETITE)).isEqualTo("cautious");
        assertThat(annotated.disposition().primaryTerm(DispositionAxis.AUTONOMY)).isEqualTo("guided");
        assertThat(annotated.disposition().primaryTerm(DispositionAxis.CONFLICT_MODE)).isEqualTo("accommodating");
        assertThat(annotated.disposition().delegation()).isTrue();
        assertThat(annotated.disposition().dispositionProfile()).hasSize(2);
        assertThat(annotated.disposition().dispositionProfile().get(0).term()).isEqualTo("collaborative");
        assertThat(annotated.disposition().dispositionProfile().get(0).weight()).isEqualTo(0.8);

        assertThat(annotated.axisVocabularies()).containsEntry(DispositionAxis.CONFLICT_MODE, "urn:casehub:vocab:thomas-kilmann");

        assertThat(annotated.capabilities()).hasSize(2);
        assertThat(annotated.capabilities().stream().map(c -> c.name()).toList())
                .containsExactlyInAnyOrder("cap-a", "cap-b");
        var capA = annotated.capabilities().stream().filter(c -> c.name().equals("cap-a")).findFirst().orElseThrow();
        assertThat(capA.description()).isEqualTo("Capability A");
        assertThat(capA.qualityHint()).isEqualTo(0.9);
        assertThat(capA.latencyHintP50Ms()).isEqualTo(2000L);
        assertThat(capA.epistemicDomains()).containsEntry("domain-a", 0.95);
        assertThat(capA.excludedDomains()).containsExactly("domain-x");

        assertThat(annotated.goals()).hasSize(2);
        assertThat(annotated.goals().get(0).name()).isEqualTo("goal-1");
        assertThat(annotated.goals().get(0).capabilities()).containsExactly("cap-a");

        assertThat(annotated.constraints()).hasSize(2);

        assertThat(annotated.templates()).hasSize(1);
        assertThat(annotated.templates().get(0).templateId()).isEqualTo("safety-primer");
        assertThat(annotated.templates().get(0).args()).containsEntry("domain", "legal");
    }

    @Test
    void repeatableGoals_directAnnotation() {
        var d = registry.findById("repeatable-agent", "test-tenant").orElseThrow();
        assertThat(d.goals()).hasSize(2);
        var speed = d.goals().stream().filter(g -> g.name().equals("speed")).findFirst().orElseThrow();
        assertThat(speed.description()).isEqualTo("Respond quickly");
        assertThat(speed.priority()).isEqualTo(GoalPriority.PRIMARY);
        var clarity = d.goals().stream().filter(g -> g.name().equals("clarity")).findFirst().orElseThrow();
        assertThat(clarity.priority()).isEqualTo(GoalPriority.SECONDARY);
    }

    @Test
    void repeatableConstraints_directAnnotation() {
        var d = registry.findById("repeatable-agent", "test-tenant").orElseThrow();
        assertThat(d.constraints()).hasSize(2);
        var noPii = d.constraints().stream().filter(c -> c.name().equals("no-pii")).findFirst().orElseThrow();
        assertThat(noPii.severity()).isEqualTo(ConstraintSeverity.HARD);
        var log = d.constraints().stream().filter(c -> c.name().equals("log-actions")).findFirst().orElseThrow();
        assertThat(log.severity()).isEqualTo(ConstraintSeverity.SOFT);
    }
}

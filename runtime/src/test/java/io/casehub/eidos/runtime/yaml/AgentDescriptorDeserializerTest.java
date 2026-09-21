package io.casehub.eidos.runtime.yaml;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentValidationException;
import io.casehub.eidos.api.ConstraintSeverity;
import io.casehub.eidos.api.DispositionAxis;
import io.casehub.eidos.core.yaml.EidosDescriptorModule;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentDescriptorDeserializerTest {

    private final ObjectMapper mapper = EidosDescriptorModule.createMapper(null);

    @Test
    void fullDescriptor_allFieldsDeserialize() throws Exception {
        var yaml = """
            agentId: test-1
            name: Test Agent
            slot: reviewer
            tenancyId: default
            version: "1.0"
            provider: acme
            modelFamily: gpt-4
            modelVersion: "2026-01"
            weightsFingerprint: sha256:abc
            domainVocabulary: urn:domain
            slotVocabulary: urn:slot
            dispositionVocabulary: urn:disp
            styleVocabulary: urn:style
            jurisdiction: EU
            dataHandlingPolicy: GDPR
            briefing: You are a test agent.
            disposition:
              socialOrient: collaborative
              delegation: true
            capabilities:
              - name: review
                description: Code review capability
                qualityHint: 0.95
                latencyHintP50Ms: 5000
                costHint: medium
                inputTypes: [code]
                outputTypes: [review]
                tags: [quality]
                epistemicDomains:
                  java: 0.95
                  rust: 0.3
                excludedDomains: [cobol]
            goals:
              - name: ensure-quality
                description: Ensure code quality
                priority: PRIMARY
                visibility: PUBLIC
                capabilities: [review]
            constraints:
              - name: no-pii
                description: Never process PII
                visibility: PUBLIC
                severity: HARD
            templates:
              - ref: safety-preamble
                args:
                  domain: healthcare
            """;
        var d = mapper.readValue(yaml, AgentDescriptor.class);
        assertThat(d.agentId()).isEqualTo("test-1");
        assertThat(d.name()).isEqualTo("Test Agent");
        assertThat(d.slot()).isEqualTo("reviewer");
        assertThat(d.tenancyId()).isEqualTo("default");
        assertThat(d.version()).isEqualTo("1.0");
        assertThat(d.provider()).isEqualTo("acme");
        assertThat(d.modelFamily()).isEqualTo("gpt-4");
        assertThat(d.modelVersion()).isEqualTo("2026-01");
        assertThat(d.weightsFingerprint()).isEqualTo("sha256:abc");
        assertThat(d.domainVocabulary()).isEqualTo("urn:domain");
        assertThat(d.slotVocabulary()).isEqualTo("urn:slot");
        assertThat(d.dispositionVocabulary()).isEqualTo("urn:disp");
        assertThat(d.styleVocabulary()).isEqualTo("urn:style");
        assertThat(d.jurisdiction()).isEqualTo("EU");
        assertThat(d.dataHandlingPolicy()).isEqualTo("GDPR");
        assertThat(d.briefing()).isEqualTo("You are a test agent.");
        assertThat(d.disposition().primaryTerm(DispositionAxis.SOCIAL_ORIENTATION)).isEqualTo("collaborative");
        assertThat(d.disposition().delegation()).isTrue();
        assertThat(d.capabilities()).hasSize(1);
        assertThat(d.capabilities().get(0).name()).isEqualTo("review");
        assertThat(d.capabilities().get(0).description()).isEqualTo("Code review capability");
        assertThat(d.capabilities().get(0).qualityHint()).isEqualTo(0.95);
        assertThat(d.capabilities().get(0).latencyHintP50Ms()).isEqualTo(5000L);
        assertThat(d.capabilities().get(0).costHint()).isEqualTo("medium");
        assertThat(d.capabilities().get(0).inputTypes()).containsExactly("code");
        assertThat(d.capabilities().get(0).outputTypes()).containsExactly("review");
        assertThat(d.capabilities().get(0).tags()).containsExactly("quality");
        assertThat(d.capabilities().get(0).epistemicDomains()).containsEntry("java", 0.95);
        assertThat(d.capabilities().get(0).excludedDomains()).containsExactly("cobol");
        assertThat(d.goals()).hasSize(1);
        assertThat(d.goals().get(0).name()).isEqualTo("ensure-quality");
        assertThat(d.goals().get(0).capabilities()).containsExactly("review");
        assertThat(d.constraints()).hasSize(1);
        assertThat(d.constraints().get(0).severity()).isEqualTo(ConstraintSeverity.HARD);
        assertThat(d.templates()).hasSize(1);
        assertThat(d.templates().get(0).templateId()).isEqualTo("safety-preamble");
        assertThat(d.templates().get(0).args()).containsEntry("domain", "healthcare");
    }

    @Test
    void minimalDescriptor_nullOptionalFields() throws Exception {
        var yaml = """
            agentId: minimal
            name: Minimal
            slot: s
            tenancyId: t
            """;
        var d = mapper.readValue(yaml, AgentDescriptor.class);
        assertThat(d.version()).isNull();
        assertThat(d.provider()).isNull();
        assertThat(d.disposition()).isNull();
        assertThat(d.capabilities()).isEmpty();
        assertThat(d.goals()).isEmpty();
        assertThat(d.constraints()).isEmpty();
    }

    @Test
    void axisVocabularies_deserializeToEnumKeys() throws Exception {
        var yaml = """
            agentId: vocab-test
            name: N
            slot: s
            tenancyId: t
            axisVocabularies:
              CONFLICT_MODE: urn:casehub:vocab:thomas-kilmann
              RULE_FOLLOWING: urn:casehub:vocab:conscientiousness
            """;
        var d = mapper.readValue(yaml, AgentDescriptor.class);
        assertThat(d.axisVocabularies())
            .containsEntry(DispositionAxis.CONFLICT_MODE, "urn:casehub:vocab:thomas-kilmann")
            .containsEntry(DispositionAxis.RULE_FOLLOWING, "urn:casehub:vocab:conscientiousness");
    }

    @Test
    void invalidAxisVocabularyKey_throws() {
        var yaml = """
            agentId: bad
            name: N
            slot: s
            tenancyId: t
            axisVocabularies:
              INVALID_AXIS: urn:foo
            """;
        assertThatThrownBy(() -> mapper.readValue(yaml, AgentDescriptor.class))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void missingRequiredField_throwsValidationException() {
        var yaml = """
            name: No ID
            slot: s
            tenancyId: t
            """;
        assertThatThrownBy(() -> mapper.readValue(yaml, AgentDescriptor.class))
            .isInstanceOf(AgentValidationException.class)
            .hasMessageContaining("agentId");
    }

    @Test
    void tenancyId_defaultsWhenAbsent() throws Exception {
        var yaml = """
            agentId: no-tenancy
            name: N
            slot: s
            """;
        var d = mapper.readValue(yaml, AgentDescriptor.class);
        assertThat(d.tenancyId()).isEqualTo("default");
    }

    @Test
    void capabilityWithVocabulary_roundtrips() throws Exception {
        var yaml = """
            agentId: cap-vocab
            name: N
            slot: s
            tenancyId: t
            capabilities:
              - name: review
                capabilityVocabulary: urn:casehub:vocab:capability
            """;
        var d = mapper.readValue(yaml, AgentDescriptor.class);
        assertThat(d.capabilities().get(0).capabilityVocabulary()).isEqualTo("urn:casehub:vocab:capability");
    }

    @Test
    void templateWithoutArgs_emptyMap() throws Exception {
        var yaml = """
            agentId: tmpl
            name: N
            slot: s
            tenancyId: t
            templates:
              - ref: closing-reminder
            """;
        var d = mapper.readValue(yaml, AgentDescriptor.class);
        assertThat(d.templates().get(0).templateId()).isEqualTo("closing-reminder");
        assertThat(d.templates().get(0).args()).isEmpty();
    }

    @Test
    void capabilityModelInlineConstraintsDeserialize() throws Exception {
        var yaml = """
                   agentId: model-test
                   name: Model Test
                   slot: tester
                   tenancyId: default
                   capabilities:
                     - name: code-review
                       model:
                         tier: FLAGSHIP
                         capabilities:
                           - text
                           - tool-use
                         max-cost: HIGH
                       qualityHint: 0.95
                     - name: lint
                       model:
                         tier: FAST
                         capabilities: [text]
                   """;

        var descriptor = mapper.readValue(yaml, AgentDescriptor.class);
        assertThat(descriptor.capabilities()).hasSize(2);

        var review = descriptor.capabilities().get(0);
        assertThat(review.model()).isNotNull();
        assertThat(review.model().tier()).isEqualTo(io.casehub.platform.api.model.ModelTier.FLAGSHIP);
        assertThat(review.model().requiredCapabilities()).containsExactlyInAnyOrder("text", "tool-use");
        assertThat(review.model().maxCostTier()).isEqualTo(io.casehub.platform.api.model.CostTier.HIGH);
        assertThat(review.modelRef()).isNull();

        var lint = descriptor.capabilities().get(1);
        assertThat(lint.model().tier()).isEqualTo(io.casehub.platform.api.model.ModelTier.FAST);
        assertThat(lint.model().requiredCapabilities()).containsExactly("text");
    }

    @Test
    void capabilityModelStringShorthandDeserialize() throws Exception {
        var yaml = """
                   agentId: model-test
                   name: Model Test
                   slot: tester
                   tenancyId: default
                   capabilities:
                     - name: review
                       model: reasoning-heavy
                   """;

        var descriptor = mapper.readValue(yaml, AgentDescriptor.class);
        var cap = descriptor.capabilities().getFirst();
        assertThat(cap.modelRef()).isEqualTo("reasoning-heavy");
        assertThat(cap.model()).isNull();
    }

    @Test
    void capabilityWithoutModelDeserializesAsNull() throws Exception {
        var yaml = """
                   agentId: no-model
                   name: No Model
                   slot: tester
                   tenancyId: default
                   capabilities:
                     - name: review
                   """;

        var descriptor = mapper.readValue(yaml, AgentDescriptor.class);
        var cap = descriptor.capabilities().getFirst();
        assertThat(cap.modelRef()).isNull();
        assertThat(cap.model()).isNull();
    }

    @Test
    void extensionData_deserializesNestedStructure() throws Exception {
        var yaml = """
                   agentId: ext-test
                   name: Extension Test
                   slot: tester
                   tenancyId: default
                   extensionData:
                     io.casehub.manor.social:
                       drives:
                         - belonging
                         - approval
                       norms:
                         formality: 0.3
                   """;
        var d = mapper.readValue(yaml, AgentDescriptor.class);
        assertThat(d.extensionData()).isNotNull();
        var social = (java.util.Map<?, ?>) d.extensionData().get("io.casehub.manor.social");
        assertThat(social).isNotNull();
        var drives = (java.util.List<?>) social.get("drives");
        assertThat(drives).isEqualTo(java.util.List.of("belonging", "approval"));
        var norms = (java.util.Map<?, ?>) social.get("norms");
        assertThat(norms.get("formality")).isEqualTo(0.3);
    }

    @Test
    void extensionData_absent_isNull() throws Exception {
        var yaml = """
                   agentId: no-ext
                   name: No Ext
                   slot: tester
                   tenancyId: default
                   """;
        var d = mapper.readValue(yaml, AgentDescriptor.class);
        assertThat(d.extensionData()).isNull();
    }

    @Test
    void archetypeAndAdjectives_deserialize() throws Exception {
        var yaml = """
                   agentId: arch-test
                   name: Archetype Test
                   slot: analyst
                   tenancyId: t1
                   archetype: detective
                   archetypeAdjectives: [meticulous, persistent]
                   """;
        var d = mapper.readValue(yaml, AgentDescriptor.class);
        assertThat(d.archetype()).isEqualTo("detective");
        assertThat(d.archetypeAdjectives()).containsExactly("meticulous", "persistent");
    }

    @Test
    void archetypeWithoutAdjectives_adjectivesDefaultToEmpty() throws Exception {
        var yaml = """
                   agentId: arch-no-adj
                   name: No Adj
                   slot: analyst
                   tenancyId: t1
                   archetype: mentor
                   """;
        var d = mapper.readValue(yaml, AgentDescriptor.class);
        assertThat(d.archetype()).isEqualTo("mentor");
        assertThat(d.archetypeAdjectives()).isEmpty();
    }

    @Test
    void noArchetype_fieldsAreNull() throws Exception {
        var yaml = """
                   agentId: no-arch
                   name: No Arch
                   slot: analyst
                   tenancyId: t1
                   """;
        var d = mapper.readValue(yaml, AgentDescriptor.class);
        assertThat(d.archetype()).isNull();
        assertThat(d.archetypeAdjectives()).isEmpty();
    }
}

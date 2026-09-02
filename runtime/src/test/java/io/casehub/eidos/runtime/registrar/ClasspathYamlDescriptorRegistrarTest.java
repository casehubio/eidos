package io.casehub.eidos.runtime.registrar;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.DispositionAxis;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClasspathYamlDescriptorRegistrarTest {

    static final ClasspathYamlDescriptorRegistrar registrar = new ClasspathYamlDescriptorRegistrar();

    List<AgentDescriptor> parse(String yaml) {
        return registrar.loadFrom(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void empty_descriptors_list_returns_empty() {
        var result = parse("descriptors: []");
        assertThat(result).isEmpty();
    }

    @Test
    void null_input_returns_empty() {
        var result = registrar.loadFrom(null);
        assertThat(result).isEmpty();
    }

    @Test
    void valid_single_descriptor_maps_all_fields() {
        var yaml = """
            descriptors:
              - agentId: test-1
                name: Test Agent
                slot: reviewer
                tenancyId: default
                version: "1.0"
                disposition:
                  conflictMode: collaborating
                  ruleFollowing: strict
                  delegation: false
                capabilities:
                  - name: code-review
                    tags: [quality]
                    inputTypes: [code]
                    outputTypes: [review]
                briefing: You are a test agent for code review.
            """;

        var result = parse(yaml);
        assertThat(result).hasSize(1);
        var d = result.get(0);
        assertThat(d.agentId()).isEqualTo("test-1");
        assertThat(d.name()).isEqualTo("Test Agent");
        assertThat(d.slot()).isEqualTo("reviewer");
        assertThat(d.tenancyId()).isEqualTo("default");
        assertThat(d.version()).isEqualTo("1.0");
        assertThat(d.disposition().primaryTerm(DispositionAxis.CONFLICT_MODE)).isEqualTo("collaborating");
        assertThat(d.disposition().primaryTerm(DispositionAxis.RULE_FOLLOWING)).isEqualTo("strict");
        assertThat(d.disposition().delegation()).isFalse();
        assertThat(d.capabilities()).hasSize(1);
        assertThat(d.capabilities().get(0).name()).isEqualTo("code-review");
        assertThat(d.capabilities().get(0).tags()).containsExactly("quality");
        assertThat(d.briefing()).isEqualTo("You are a test agent for code review.");
    }

    @Test
    void optional_fields_default_to_null() {
        var yaml = """
            descriptors:
              - agentId: minimal
                name: Minimal
                slot: s
                tenancyId: t
            """;

        var result = parse(yaml);
        assertThat(result).hasSize(1);
        var d = result.get(0);
        assertThat(d.version()).isNull();
        assertThat(d.provider()).isNull();
        assertThat(d.briefing()).isNull();
        assertThat(d.disposition()).isNull();
        assertThat(d.capabilities()).isEmpty();
    }

    @Test
    void axis_vocabularies_deserialised_to_enum_keys() {
        var yaml = """
            descriptors:
              - agentId: vocab-test
                name: N
                slot: s
                tenancyId: t
                axisVocabularies:
                  CONFLICT_MODE: urn:casehub:vocab:thomas-kilmann
                  RULE_FOLLOWING: urn:casehub:vocab:conscientiousness
            """;

        var result = parse(yaml);
        var axisVocabs = result.get(0).axisVocabularies();
        assertThat(axisVocabs).containsEntry(DispositionAxis.CONFLICT_MODE,
            "urn:casehub:vocab:thomas-kilmann");
        assertThat(axisVocabs).containsEntry(DispositionAxis.RULE_FOLLOWING,
            "urn:casehub:vocab:conscientiousness");
    }

    @Test
    void invalid_axis_vocabulary_key_throws() {
        var yaml = """
            descriptors:
              - agentId: bad
                name: N
                slot: s
                tenancyId: t
                axisVocabularies:
                  INVALID_AXIS: urn:foo
            """;

        assertThatThrownBy(() -> parse(yaml))
            .isInstanceOf(IllegalStateException.class)
            .hasRootCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void missing_required_field_throws_validation_exception() {
        var yaml = """
            descriptors:
              - name: No ID
                slot: s
                tenancyId: t
            """;

        assertThatThrownBy(() -> parse(yaml))
            .isInstanceOf(IllegalStateException.class)
            .hasRootCauseInstanceOf(io.casehub.eidos.api.AgentValidationException.class)
            .hasMessageContaining("agentId");
    }

    @Test
    void capability_with_epistemic_domains_and_excluded_domains() {
        var yaml = """
            descriptors:
              - agentId: epistemic
                name: N
                slot: s
                tenancyId: t
                capabilities:
                  - name: review
                    epistemicDomains:
                      java: 0.95
                      rust: 0.3
                    excludedDomains: [cobol]
            """;

        var result = parse(yaml);
        var cap = result.get(0).capabilities().get(0);
        assertThat(cap.epistemicDomains()).containsEntry("java", 0.95);
        assertThat(cap.excludedDomains()).containsExactly("cobol");
    }

    @Test
    void goals_and_constraints_deserialized() {
        var yaml = """
                   descriptors:
                     - agentId: hooded-claw
                       name: The Hooded Claw
                       slot: villain
                       tenancyId: wacky-manor
                       goals:
                         - name: eliminate-penelope
                           description: "Kill Penelope Pitstop"
                           priority: PRIMARY
                           visibility: PRIVATE
                         - name: win-treasure
                           description: "Win the treasure hunt"
                           priority: SECONDARY
                           visibility: PUBLIC
                       constraints:
                         - name: never-break-cover
                           description: "Never reveal your true identity"
                           visibility: PRIVATE
                           severity: HARD
                         - name: elaborate-schemes
                           description: "Schemes must be elaborate"
                           visibility: PUBLIC
                           severity: SOFT
                   """;
        var result = parse(yaml);
        assertThat(result).hasSize(1);
        var d = result.get(0);
        assertThat(d.goals()).hasSize(2);
        assertThat(d.goals().get(0).name()).isEqualTo("eliminate-penelope");
        assertThat(d.goals().get(0).priority()).isEqualTo(io.casehub.eidos.api.GoalPriority.PRIMARY);
        assertThat(d.goals().get(0).visibility()).isEqualTo(io.casehub.eidos.api.Visibility.PRIVATE);
        assertThat(d.goals().get(1).priority()).isEqualTo(io.casehub.eidos.api.GoalPriority.SECONDARY);
        assertThat(d.constraints()).hasSize(2);
        assertThat(d.constraints().get(0).name()).isEqualTo("never-break-cover");
        assertThat(d.constraints().get(0).visibility()).isEqualTo(io.casehub.eidos.api.Visibility.PRIVATE);
    }

    @Test
    void missing_goals_and_constraints_defaults_to_empty() {
        var yaml = """
                   descriptors:
                     - agentId: minimal
                       name: Minimal
                       slot: s
                       tenancyId: t
                   """;
        var result = parse(yaml);
        assertThat(result.get(0).goals()).isEmpty();
        assertThat(result.get(0).constraints()).isEmpty();
    }

    @Test
    void goal_missing_visibility_defaults_to_public() {
        var yaml = """
                   descriptors:
                     - agentId: defaults-test
                       name: N
                       slot: s
                       tenancyId: t
                       goals:
                         - name: g
                           description: d
                   """;
        var result = parse(yaml);
        assertThat(result).hasSize(1);
        var goal = result.get(0).goals().get(0);
        assertThat(goal.visibility()).isEqualTo(io.casehub.eidos.api.Visibility.PUBLIC);
        assertThat(goal.priority()).isEqualTo(io.casehub.eidos.api.GoalPriority.PRIMARY);
    }

    @Test
    void dispositionProfile_parsed_from_yaml() {
        var yaml = """
                   descriptors:
                     - agentId: jungian-test
                       name: Test
                       slot: s
                       tenancyId: t
                       dispositionVocabulary: urn:casehub:vocab:jungian
                       disposition:
                         dispositionProfile:
                           - term: te
                             weight: 0.35
                           - term: ni
                             weight: 0.20
                   """;
        var result = parse(yaml);
        assertThat(result).hasSize(1);
        var profile = result.get(0).disposition().dispositionProfile();
        assertThat(profile).hasSize(2);
        assertThat(profile.get(0).term()).isEqualTo("te");
        assertThat(profile.get(0).weight()).isEqualTo(0.35);
        assertThat(profile.get(1).term()).isEqualTo("ni");
        assertThat(profile.get(1).weight()).isEqualTo(0.20);
    }

    @Test
    void mbtiType_resolved_to_dispositionProfile() {
        var yaml = """
                   descriptors:
                     - agentId: mbti-test
                       name: Test
                       slot: s
                       tenancyId: t
                       dispositionVocabulary: urn:casehub:vocab:jungian
                       disposition:
                         mbtiType: ENTJ
                   """;
        var result = registrar.loadFrom(
                new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)),
                testVocabRegistry());
        assertThat(result).hasSize(1);
        var profile = result.get(0).disposition().dispositionProfile();
        assertThat(profile).hasSize(8);
        assertThat(profile.get(0).term()).isEqualTo("te");
        assertThat(profile.get(0).weight()).isEqualTo(0.35);
    }

    @Test
    void mbtiType_case_insensitive() {
        var yaml = """
                   descriptors:
                     - agentId: case-test
                       name: Test
                       slot: s
                       tenancyId: t
                       dispositionVocabulary: urn:casehub:vocab:jungian
                       disposition:
                         mbtiType: entj
                   """;
        var result = registrar.loadFrom(
                new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)),
                testVocabRegistry());
        var profile = result.get(0).disposition().dispositionProfile();
        assertThat(profile).hasSize(8);
        assertThat(profile.get(0).term()).isEqualTo("te");
    }

    @Test
    void explicit_dispositionProfile_wins_over_mbtiType() {
        var yaml = """
                   descriptors:
                     - agentId: both-test
                       name: Test
                       slot: s
                       tenancyId: t
                       dispositionVocabulary: urn:casehub:vocab:jungian
                       disposition:
                         mbtiType: ENTJ
                         dispositionProfile:
                           - term: ti
                             weight: 0.50
                           - term: ne
                             weight: 0.50
                   """;
        var result  = parse(yaml);
        var profile = result.get(0).disposition().dispositionProfile();
        assertThat(profile).hasSize(2);
        assertThat(profile.get(0).term()).isEqualTo("ti");
        assertThat(profile.get(0).weight()).isEqualTo(0.50);
    }

    @Test
    void enneagramType_projects_axes_without_overwriting_explicit_values() {
        var yaml = """
                   descriptors:
                     - agentId: ennea-test
                       name: Test
                       slot: s
                       tenancyId: t
                       disposition:
                         enneagramType: challenger
                         socialOrient: collaborative
                   """;
        var result = registrar.loadFrom(
                new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)),
                testVocabRegistry());
        assertThat(result).hasSize(1);
        var d = result.get(0).disposition();
        assertThat(d.primaryTerm(DispositionAxis.SOCIAL_ORIENTATION)).isEqualTo("collaborative");
        assertThat(d.primaryTerm(DispositionAxis.RULE_FOLLOWING)).isEqualTo("flexible");
        assertThat(d.primaryTerm(DispositionAxis.RISK_APPETITE)).isEqualTo("bold");
        assertThat(d.primaryTerm(DispositionAxis.AUTONOMY)).isEqualTo("autonomous");
        assertThat(d.primaryTerm(DispositionAxis.CONFLICT_MODE)).isEqualTo("competing");
    }

    @Test
    void enneagramType_complements_mbtiType_without_overwriting_profile() {
        var yaml = """
                   descriptors:
                     - agentId: both-ennea-mbti
                       name: Test
                       slot: s
                       tenancyId: t
                       dispositionVocabulary: urn:casehub:vocab:jungian
                       disposition:
                         mbtiType: INTJ
                         enneagramType: reformer
                   """;
        var result = registrar.loadFrom(
                new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)),
                testVocabRegistry());
        assertThat(result).hasSize(1);
        var d = result.get(0).disposition();
        assertThat(d.dispositionProfile()).hasSize(8);
        assertThat(d.dispositionProfile().get(0).term()).isEqualTo("ni");
        assertThat(d.primaryTerm(DispositionAxis.RULE_FOLLOWING)).isEqualTo("strict");
    }

    @Test
    void identity_fields_all_roundtrip() {
        var yaml = """
                   descriptors:
                     - agentId: full-identity
                       name: Full Identity Agent
                       slot: analyst
                       tenancyId: t1
                       version: "2.1"
                       provider: acme-ai
                       modelFamily: gpt-4
                       modelVersion: "2026-01"
                       weightsFingerprint: sha256:abc123
                       domainVocabulary: urn:acme:vocab:domain
                       slotVocabulary: urn:acme:vocab:slot
                       dispositionVocabulary: urn:acme:vocab:disposition
                       styleVocabulary: urn:acme:vocab:style
                       jurisdiction: EU
                       dataHandlingPolicy: GDPR-compliant
                       briefing: You are an analyst specialising in risk.
                   """;
        var result = parse(yaml);
        assertThat(result).hasSize(1);
        var d = result.get(0);
        assertThat(d.agentId()).isEqualTo("full-identity");
        assertThat(d.provider()).isEqualTo("acme-ai");
        assertThat(d.modelFamily()).isEqualTo("gpt-4");
        assertThat(d.modelVersion()).isEqualTo("2026-01");
        assertThat(d.weightsFingerprint()).isEqualTo("sha256:abc123");
        assertThat(d.domainVocabulary()).isEqualTo("urn:acme:vocab:domain");
        assertThat(d.slotVocabulary()).isEqualTo("urn:acme:vocab:slot");
        assertThat(d.dispositionVocabulary()).isEqualTo("urn:acme:vocab:disposition");
        assertThat(d.styleVocabulary()).isEqualTo("urn:acme:vocab:style");
        assertThat(d.jurisdiction()).isEqualTo("EU");
        assertThat(d.dataHandlingPolicy()).isEqualTo("GDPR-compliant");
    }

    @Test
    void goal_description_and_capabilities_roundtrip() {
        var yaml = """
                   descriptors:
                     - agentId: goal-caps
                       name: Goal Caps Agent
                       slot: planner
                       tenancyId: t1
                       capabilities:
                         - name: code-review
                         - name: testing
                       goals:
                         - name: ensure-quality
                           description: Ensure all code meets quality standards
                           priority: PRIMARY
                           visibility: PUBLIC
                           capabilities:
                             - code-review
                             - testing
                         - name: mentor-juniors
                           description: Help junior developers grow
                           priority: SECONDARY
                           visibility: PUBLIC
                   """;
        var result = parse(yaml);
        var d      = result.get(0);
        assertThat(d.goals()).hasSize(2);
        var g1 = d.goals().get(0);
        assertThat(g1.name()).isEqualTo("ensure-quality");
        assertThat(g1.description()).isEqualTo("Ensure all code meets quality standards");
        assertThat(g1.capabilities()).containsExactly("code-review", "testing");
        var g2 = d.goals().get(1);
        assertThat(g2.description()).isEqualTo("Help junior developers grow");
        assertThat(g2.capabilities()).isEmpty();
    }

    @Test
    void constraint_description_and_severity_roundtrip() {
        var yaml = """
                   descriptors:
                     - agentId: constraint-test
                       name: Constrained Agent
                       slot: worker
                       tenancyId: t1
                       constraints:
                         - name: no-pii
                           description: Never process personally identifiable information
                           visibility: PUBLIC
                           severity: HARD
                         - name: prefer-short-responses
                           description: Keep responses concise when possible
                           visibility: PUBLIC
                           severity: SOFT
                   """;
        var result = parse(yaml);
        var d      = result.get(0);
        assertThat(d.constraints()).hasSize(2);
        var c1 = d.constraints().get(0);
        assertThat(c1.name()).isEqualTo("no-pii");
        assertThat(c1.description()).isEqualTo("Never process personally identifiable information");
        assertThat(c1.severity()).isEqualTo(io.casehub.eidos.api.ConstraintSeverity.HARD);
        var c2 = d.constraints().get(1);
        assertThat(c2.description()).isEqualTo("Keep responses concise when possible");
        assertThat(c2.severity()).isEqualTo(io.casehub.eidos.api.ConstraintSeverity.SOFT);
    }

    @Test
    void templates_roundtrip() {
        var yaml = """
                   descriptors:
                     - agentId: template-test
                       name: Template Agent
                       slot: worker
                       tenancyId: t1
                       templates:
                         - ref: safety-preamble
                           args:
                             domain: healthcare
                             severity: critical
                         - ref: closing-reminder
                   """;
        var result = parse(yaml);
        var d      = result.get(0);
        assertThat(d.templates()).hasSize(2);
        var t1 = d.templates().get(0);
        assertThat(t1.templateId()).isEqualTo("safety-preamble");
        assertThat(t1.args()).containsEntry("domain", "healthcare");
        assertThat(t1.args()).containsEntry("severity", "critical");
        var t2 = d.templates().get(1);
        assertThat(t2.templateId()).isEqualTo("closing-reminder");
        assertThat(t2.args()).isEmpty();
    }

    @Test
    void disposition_styleProfile_roundtrip() {
        var yaml = """
                   descriptors:
                     - agentId: style-test
                       name: Style Agent
                       slot: writer
                       tenancyId: t1
                       styleVocabulary: urn:acme:vocab:style
                       disposition:
                         styleProfile:
                           - term: concise
                             weight: 0.60
                           - term: formal
                             weight: 0.40
                   """;
        var result = parse(yaml);
        var d      = result.get(0);
        var style  = d.disposition().styleProfile();
        assertThat(style).hasSize(2);
        assertThat(style.get(0).term()).isEqualTo("concise");
        assertThat(style.get(0).weight()).isEqualTo(0.60);
        assertThat(style.get(1).term()).isEqualTo("formal");
        assertThat(style.get(1).weight()).isEqualTo(0.40);
    }


    // --- yaml-core preprocessing integration tests ---

    @Test
    void variable_resolution_produces_correct_descriptors() {
        var yaml = """
                variables:
                  tenant: wacky-races
                descriptors:
                  - agentId: racer
                    name: Racer
                    slot: driver
                    tenancyId: ${var.tenant}
                """;
        var result = parse(yaml);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).tenancyId()).isEqualTo("wacky-races");
    }

    @Test
    void forEach_expansion_produces_multiple_descriptors() {
        var yaml = """
                iterations:
                  teams:
                    as: team
                    in: [frontend, backend]
                descriptors:
                  - agentId: ${each.team}-reviewer
                    name: ${each.team} Reviewer
                    slot: reviewer
                    tenancyId: default
                    forEach: teams
                    capabilities:
                      - name: code-review
                        tags: ["${each.team}"]
                """;
        var result = parse(yaml);
        assertThat(result).hasSize(2);
        assertThat(result.get(0).agentId()).isEqualTo("frontend-reviewer");
        assertThat(result.get(0).capabilities().get(0).tags())
                .containsExactly("frontend");
        assertThat(result.get(1).agentId()).isEqualTo("backend-reviewer");
    }

    @Test
    void when_false_excludes_from_result() {
        var yaml = """
                variables:
                  audit_enabled: "false"
                descriptors:
                  - agentId: always
                    name: Always
                    slot: s
                    tenancyId: t
                  - agentId: gated
                    name: Gated
                    slot: s
                    tenancyId: t
                    when: "${var.audit_enabled}"
                """;
        var result = parse(yaml);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).agentId()).isEqualTo("always");
    }

    @Test
    void csv_inline_expansion_produces_correct_descriptors() {
        var yaml = """
                dataSources:
                  roster:
                    csv: |
                      name:STRING, role:STRING
                      alice, reviewer
                      bob, planner
                descriptors:
                  - agentId: ${each.agent.name}-agent
                    name: ${each.agent.name}
                    slot: ${each.agent.role}
                    tenancyId: default
                    forEach:
                      as: agent
                      in: roster
                """;
        var result = parse(yaml);
        assertThat(result).hasSize(2);
        assertThat(result.get(0).agentId()).isEqualTo("alice-agent");
        assertThat(result.get(0).slot()).isEqualTo("reviewer");
        assertThat(result.get(1).agentId()).isEqualTo("bob-agent");
        assertThat(result.get(1).slot()).isEqualTo("planner");
    }

    @Test
    void existing_yaml_without_preprocessing_works_unchanged() {
        var yaml = """
                descriptors:
                  - agentId: plain
                    name: Plain Agent
                    slot: worker
                    tenancyId: default
                    disposition:
                      conflictMode: collaborating
                      delegation: false
                    capabilities:
                      - name: code-review
                        tags: [quality]
                    briefing: You are a plain agent.
                """;
        var result = parse(yaml);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).agentId()).isEqualTo("plain");
        assertThat(result.get(0).disposition().primaryTerm(
                io.casehub.eidos.api.DispositionAxis.CONFLICT_MODE))
                .isEqualTo("collaborating");
        assertThat(result.get(0).capabilities().get(0).name())
                .isEqualTo("code-review");
    }

    private io.casehub.eidos.api.VocabularyRegistry testVocabRegistry() {
        var registry = new io.casehub.eidos.runtime.vocabulary.CdiVocabularyRegistry();
        registry.register(io.casehub.eidos.vocab.JungianFunctionTerm.class);
        registry.register(io.casehub.eidos.vocab.MbtiTypeTerm.class);
        registry.register(io.casehub.eidos.vocab.EnneagramTerm.class);
        registry.register(io.casehub.eidos.vocab.ConscientiousnessTerm.class);
        registry.register(io.casehub.eidos.vocab.ThomasKilmannTerm.class);
        return registry;
    }
}

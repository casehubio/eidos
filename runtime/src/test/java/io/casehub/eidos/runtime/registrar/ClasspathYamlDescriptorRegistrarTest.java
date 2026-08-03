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
            .isInstanceOf(IllegalArgumentException.class);
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
            .isInstanceOf(io.casehub.eidos.api.AgentValidationException.class)
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
    void goal_missing_visibility_throws() {
        var yaml = """
                   descriptors:
                     - agentId: bad
                       name: N
                       slot: s
                       tenancyId: t
                       goals:
                         - name: g
                           description: d
                           priority: PRIMARY
                   """;
        assertThatThrownBy(() -> parse(yaml))
                .isInstanceOf(NullPointerException.class);
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

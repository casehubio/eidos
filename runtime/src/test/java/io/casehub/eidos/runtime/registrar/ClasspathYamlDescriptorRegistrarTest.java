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
        assertThat(d.disposition().conflictMode()).isEqualTo("collaborating");
        assertThat(d.disposition().ruleFollowing()).isEqualTo("strict");
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
}

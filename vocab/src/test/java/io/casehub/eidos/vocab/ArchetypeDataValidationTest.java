package io.casehub.eidos.vocab;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class ArchetypeDataValidationTest {

    private static final String YAML_PATH = "META-INF/eidos/archetypes.yaml";
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    private static JsonNode root;

    @BeforeAll
    static void loadYaml() throws IOException {
        var stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(YAML_PATH);
        assertThat(stream).as("archetypes.yaml must be on the classpath").isNotNull();
        root = YAML.readTree(stream);
    }

    @Test
    void yaml_contains_all_12_families() {
        var yamlFamilies = fieldNames(root.get("families"));
        var javaFamilies = Arrays.stream(ArchetypeFamily.values())
                .map(f -> f.name().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        assertThat(yamlFamilies).containsExactlyInAnyOrderElementsOf(javaFamilies);
    }

    @Test
    void yaml_family_quadrants_match_java() {
        var families = root.get("families");
        for (var family : ArchetypeFamily.values()) {
            var key = family.name().toLowerCase(Locale.ROOT);
            var node = families.get(key);
            assertThat(node).as("family '%s' missing from YAML", key).isNotNull();
            assertThat(node.get("quadrant").asText())
                    .as("quadrant for family '%s'", key)
                    .isEqualTo(family.quadrant().name().toLowerCase(Locale.ROOT));
        }
    }

    @Test
    void yaml_contains_all_48_sub_archetypes() {
        var yamlSubs = new HashSet<String>();
        root.get("families").fields().forEachRemaining(family ->
                family.getValue().get("subArchetypes").fields().forEachRemaining(sub ->
                        yamlSubs.add(sub.getKey())));
        var javaSubs = Arrays.stream(ArchetypeTerm.values())
                .map(ArchetypeTerm::value)
                .collect(Collectors.toSet());
        assertThat(yamlSubs).containsExactlyInAnyOrderElementsOf(javaSubs);
    }

    @Test
    void yaml_sub_archetype_families_match_java() {
        for (var term : ArchetypeTerm.values()) {
            var familyKey = term.family().name().toLowerCase(Locale.ROOT);
            var familyNode = root.get("families").get(familyKey);
            assertThat(familyNode).as("family '%s' for term '%s'", familyKey, term.value()).isNotNull();
            var subNode = familyNode.get("subArchetypes").get(term.value());
            assertThat(subNode)
                    .as("sub-archetype '%s' should be under family '%s'", term.value(), familyKey)
                    .isNotNull();
        }
    }

    @Test
    void yaml_sub_archetype_descriptions_match_java() {
        for (var term : ArchetypeTerm.values()) {
            var familyKey = term.family().name().toLowerCase(Locale.ROOT);
            var subNode = root.get("families").get(familyKey).get("subArchetypes").get(term.value());
            assertThat(subNode.get("description").asText())
                    .as("description for '%s'", term.value())
                    .isEqualTo(term.description());
        }
    }

    @Test
    void yaml_valid_adjectives_match_java() {
        for (var term : ArchetypeTerm.values()) {
            var familyKey = term.family().name().toLowerCase(Locale.ROOT);
            var subNode = root.get("families").get(familyKey).get("subArchetypes").get(term.value());
            var yamlAdj = toStringList(subNode.get("validAdjectives"));
            assertThat(yamlAdj)
                    .as("validAdjectives for '%s'", term.value())
                    .containsExactlyElementsOf(term.validAdjectives());
        }
    }

    @Test
    void yaml_invalid_adjectives_match_java() {
        for (var term : ArchetypeTerm.values()) {
            var familyKey = term.family().name().toLowerCase(Locale.ROOT);
            var subNode = root.get("families").get(familyKey).get("subArchetypes").get(term.value());
            var yamlAdj = toStringList(subNode.get("invalidAdjectives"));
            assertThat(yamlAdj)
                    .as("invalidAdjectives for '%s'", term.value())
                    .containsExactlyElementsOf(term.invalidAdjectives());
        }
    }

    @Test
    void yaml_compatibility_mbti_matches_java() {
        var mbtiNode = root.get("compatibility").get("mbti");
        assertThat(mbtiNode).as("compatibility.mbti section").isNotNull();

        var mbtiTypes = List.of("INTJ", "INTP", "ENTJ", "ENTP", "INFJ", "INFP", "ENFJ", "ENFP",
                "ISTJ", "ISFJ", "ESTJ", "ESFJ", "ISTP", "ISFP", "ESTP", "ESFP");

        for (var type : mbtiTypes) {
            var yamlFamilies = toStringList(mbtiNode.get(type));
            var javaFamilies = ArchetypeCompatibility.compatibleFamilies(MbtiTypeTerm.URI, type.toLowerCase(Locale.ROOT))
                    .stream().map(f -> f.name().toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
            assertThat(new HashSet<>(yamlFamilies))
                    .as("MBTI compatibility for '%s'", type)
                    .isEqualTo(javaFamilies);
        }
    }

    @Test
    void yaml_compatibility_enneagram_matches_java() {
        var node = root.get("compatibility").get("enneagram");
        assertThat(node).as("compatibility.enneagram section").isNotNull();

        for (int i = 1; i <= 9; i++) {
            var key = "type-" + i;
            var yamlFamilies = toStringList(node.get(key));
            var javaFamilies = ArchetypeCompatibility.compatibleFamilies(EnneagramTerm.URI, key)
                    .stream().map(f -> f.name().toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
            assertThat(new HashSet<>(yamlFamilies))
                    .as("Enneagram compatibility for '%s'", key)
                    .isEqualTo(javaFamilies);
        }
    }

    @Test
    void yaml_sub_archetype_affinities_match_java() {
        var errors = new ArrayList<String>();
        for (var term : ArchetypeTerm.values()) {
            var familyKey = term.family().name().toLowerCase(Locale.ROOT);
            var subNode = root.get("families").get(familyKey).get("subArchetypes").get(term.value());
            var affinities = subNode.get("affinities");
            if (affinities == null) continue;

            if (affinities.has("mbti")) {
                var yamlMbti = toStringList(affinities.get("mbti"));
                for (var mbti : yamlMbti) {
                    boolean javaHas = ArchetypeCompatibility.subArchetypeAffinity(
                            term, MbtiTypeTerm.URI, mbti.toLowerCase(Locale.ROOT));
                    if (!javaHas) {
                        errors.add(term.value() + " MBTI affinity '" + mbti + "' in YAML but not Java");
                    }
                }
            }
            if (affinities.has("enneagram")) {
                var yamlEnn = affinities.get("enneagram");
                for (var enn : yamlEnn) {
                    String key = "type-" + enn.asText();
                    boolean javaHas = ArchetypeCompatibility.subArchetypeAffinity(
                            term, EnneagramTerm.URI, key);
                    if (!javaHas) {
                        errors.add(term.value() + " Enneagram affinity '" + key + "' in YAML but not Java");
                    }
                }
            }
        }
        if (!errors.isEmpty()) {
            fail("Affinity mismatches:\n" + String.join("\n", errors));
        }
    }

    @Test
    void yaml_disposition_axes_cover_all_five() {
        var axes = root.get("dispositionAxes");
        assertThat(axes).as("dispositionAxes section").isNotNull();
        assertThat(fieldNames(axes)).containsExactlyInAnyOrder(
                "socialOrientation", "ruleFollowing", "riskAppetite", "autonomy", "conflictMode");
    }

    @Test
    void yaml_disposition_signals_have_valid_axes() {
        var validAxes = Set.of("socialOrientation", "ruleFollowing", "riskAppetite", "autonomy", "conflictMode");
        var signals = root.get("dispositionSignals");
        assertThat(signals).as("dispositionSignals section").isNotNull();

        signals.fields().forEachRemaining(framework -> {
            framework.getValue().fields().forEachRemaining(value -> {
                value.getValue().fields().forEachRemaining(axis -> {
                    assertThat(validAxes).as("axis '%s' in framework '%s' value '%s'",
                            axis.getKey(), framework.getKey(), value.getKey())
                            .contains(axis.getKey());
                });
            });
        });
    }

    @Test
    void yaml_tendency_rules_reference_valid_axes() {
        var validAxes = Set.of("socialOrientation", "ruleFollowing", "riskAppetite", "autonomy", "conflictMode");
        var rules = root.get("tendencyRules");
        assertThat(rules).as("tendencyRules section").isNotNull();
        assertThat(rules.isArray()).isTrue();

        for (var rule : rules) {
            var conditions = rule.get("conditions");
            assertThat(conditions).as("conditions for rule '%s'", rule.get("name").asText()).isNotNull();
            conditions.fields().forEachRemaining(cond ->
                    assertThat(validAxes).as("axis '%s' in rule '%s'", cond.getKey(), rule.get("name").asText())
                            .contains(cond.getKey()));
        }
    }

    @Test
    void yaml_profession_presets_reference_valid_archetypes() {
        var validArchetypes = new HashSet<String>();
        root.get("families").fields().forEachRemaining(family ->
                family.getValue().get("subArchetypes").fields().forEachRemaining(sub ->
                        validArchetypes.add(family.getKey() + "/" + sub.getKey())));

        var presets = root.get("professionPresets");
        assertThat(presets).as("professionPresets section").isNotNull();

        var errors = new ArrayList<String>();
        for (var profession : presets) {
            for (var role : profession.get("roles")) {
                for (var variant : role.get("variants")) {
                    var archKey = variant.get("archetype").asText();
                    if (!validArchetypes.contains(archKey)) {
                        errors.add(profession.get("profession").asText() + "/" +
                                role.get("role").asText() + " references invalid archetype: " + archKey);
                    }
                }
            }
        }
        if (!errors.isEmpty()) {
            fail("Invalid archetype references:\n" + String.join("\n", errors));
        }
    }

    @Test
    void yaml_compatibility_framework_keys_are_known() {
        var knownFrameworks = Set.of("mbti", "enneagram", "disc", "belbin", "bigFive", "sdi");
        var yamlFrameworks = fieldNames(root.get("compatibility"));
        assertThat(yamlFrameworks).isSubsetOf(knownFrameworks);
    }

    @Test
    void yaml_compatibility_family_values_are_valid_enums() {
        var validFamilies = Arrays.stream(ArchetypeFamily.values())
                .map(f -> f.name().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        root.get("compatibility").fields().forEachRemaining(framework ->
            framework.getValue().fields().forEachRemaining(entry ->
                entry.getValue().forEach(f ->
                    assertThat(f.asText())
                        .as("compatibility.%s.%s value '%s'", framework.getKey(), entry.getKey(), f.asText())
                        .isIn(validFamilies)
                )
            )
        );
    }

    @Test
    void descriptor_schema_archetype_enum_matches_yaml() throws IOException {
        var schemaStream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("META-INF/eidos/descriptor.schema.json");
        assertThat(schemaStream).as("descriptor.schema.json must be on classpath").isNotNull();
        var json = new com.fasterxml.jackson.databind.ObjectMapper();
        var schema = json.readTree(schemaStream);

        var schemaArchetypes = new HashSet<String>();
        schema.at("/$defs/AgentDescriptor/properties/archetype/enum")
              .forEach(n -> schemaArchetypes.add(n.asText()));

        var yamlArchetypes = new HashSet<String>();
        root.get("families").fields().forEachRemaining(family ->
                family.getValue().get("subArchetypes").fields().forEachRemaining(sub ->
                        yamlArchetypes.add(sub.getKey())));

        assertThat(schemaArchetypes)
                .as("descriptor.schema.json archetype enum must match archetypes.yaml")
                .containsExactlyInAnyOrderElementsOf(yamlArchetypes);
    }

    private static Set<String> fieldNames(JsonNode node) {
        return StreamSupport.stream(
                ((Iterable<Map.Entry<String, JsonNode>>) () -> node.fields()).spliterator(), false)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private static List<String> toStringList(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray()) return List.of();
        return StreamSupport.stream(arrayNode.spliterator(), false)
                .map(JsonNode::asText)
                .toList();
    }
}

package io.casehub.eidos.vocab;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PersonalityVocabManifestTest {

    private final Map<String, Object> manifest = PersonalityVocabManifestGenerator.generate();

    @Test
    void topLevelKeysPresent() {
        assertThat(manifest).containsKeys(
                "dispositionAxes", "axisValues",
                "jungian", "mbti", "enneagram", "disc", "belbin", "bigFive", "sdi",
                "archetype",
                "axisMappings",
                "archetypeCompatibility", "archetypeAffinities");
    }

    @Test
    void dispositionAxesComplete() {
        var axes = asList(manifest.get("dispositionAxes"));
        assertThat(axes).hasSize(5);

        var first = asMap(axes.get(0));
        assertThat(first).containsEntry("key", "socialOrient");
        assertThat(first).containsKeys("displayName", "description", "lowPole", "highPole");
    }

    @Test
    void axisValuesContainBothVocabs() {
        var axisValues = asMap(manifest.get("axisValues"));
        assertThat(axisValues).containsKeys("conscientiousness", "thomasKilmann");

        var c = asMap(axisValues.get("conscientiousness"));
        assertThat(c).containsEntry("uri", ConscientiousnessTerm.URI);
        assertThat(asList(c.get("terms"))).hasSize(ConscientiousnessTerm.values().length);

        var tk = asMap(axisValues.get("thomasKilmann"));
        assertThat(tk).containsEntry("uri", ThomasKilmannTerm.URI);
        assertThat(asList(tk.get("terms"))).hasSize(ThomasKilmannTerm.values().length);
    }

    @Test
    void jungianTermCount() {
        var terms = asList(manifest.get("jungian"));
        assertThat(terms).hasSize(JungianFunctionTerm.values().length);

        var ti = asMap(terms.get(0));
        assertThat(ti).containsEntry("value", "ti");
        assertThat(ti).containsKeys("category", "attitude", "shadow",
                "responseStyleGuidance", "antiPatternWarning");
    }

    @Test
    void jungianShadowPairsSymmetric() {
        var terms = asList(manifest.get("jungian"));
        for (var raw : terms) {
            var term = asMap(raw);
            String value = (String) term.get("value");
            String shadow = (String) term.get("shadow");
            var shadowTerm = terms.stream()
                    .map(PersonalityVocabManifestTest::asMap)
                    .filter(t -> t.get("value").equals(shadow))
                    .findFirst().orElseThrow();
            assertThat(shadowTerm.get("shadow"))
                    .as("shadow of %s's shadow should be %s", value, value)
                    .isEqualTo(value);
        }
    }

    @Test
    void mbtiTermCount() {
        var terms = asList(manifest.get("mbti"));
        assertThat(terms).hasSize(MbtiTypeTerm.values().length);

        var intj = asMap(terms.get(0));
        assertThat(intj).containsEntry("value", "intj");
        assertThat(intj).containsEntry("dominant", "ni");
        assertThat(intj).containsEntry("auxiliary", "te");
        assertThat(intj).containsKey("defaultProfile");

        var profile = asMap(intj.get("defaultProfile"));
        assertThat(profile).hasSize(8);
        double sum = profile.values().stream()
                .mapToDouble(v -> ((Number) v).doubleValue())
                .sum();
        assertThat(sum).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    void enneagramTermCount() {
        assertThat(asList(manifest.get("enneagram"))).hasSize(EnneagramTerm.values().length);
    }

    @Test
    void discTermCount() {
        assertThat(asList(manifest.get("disc"))).hasSize(DiscTerm.values().length);
    }

    @Test
    void belbinTermCount() {
        assertThat(asList(manifest.get("belbin"))).hasSize(BelbinTerm.values().length);
    }

    @Test
    void bigFiveTermCount() {
        assertThat(asList(manifest.get("bigFive"))).hasSize(BigFiveTerm.values().length);
    }

    @Test
    void sdiTermCount() {
        assertThat(asList(manifest.get("sdi"))).hasSize(SdiTerm.values().length);
    }

    @Test
    void archetypeSection() {
        var archetype = asMap(manifest.get("archetype"));
        assertThat(archetype).containsKeys("families", "terms");
        assertThat(asList(archetype.get("families"))).hasSize(ArchetypeFamily.values().length);
        assertThat(asList(archetype.get("terms"))).hasSize(ArchetypeTerm.values().length);

        var angel = asMap(asList(archetype.get("terms")).get(0));
        assertThat(angel).containsEntry("value", "angel");
        assertThat(angel).containsEntry("family", "CAREGIVER");
        assertThat(angel).containsKeys("validAdjectives", "invalidAdjectives");
    }

    @Test
    void axisMappingsContainAllFrameworks() {
        var mappings = asMap(manifest.get("axisMappings"));
        assertThat(mappings).containsKeys(
                "jungian", "mbti", "enneagram", "disc", "belbin", "bigFive", "sdi");

        var jungianMap = asMap(mappings.get("jungian"));
        assertThat(jungianMap).hasSize(JungianFunctionTerm.values().length);

        var tiMapping = asMap(jungianMap.get("ti"));
        assertThat(tiMapping).containsEntry("socialOrient", "independent");
        assertThat(tiMapping).containsEntry("conflictMode", "avoiding");
    }

    @Test
    void mbtiAxisMappingsDerivedFromDominant() {
        var mappings = asMap(manifest.get("axisMappings"));
        var mbtiMap = asMap(mappings.get("mbti"));
        var jungianMap = asMap(mappings.get("jungian"));

        var intjMapping = asMap(mbtiMap.get("intj"));
        var niMapping = asMap(jungianMap.get("ni"));
        assertThat(intjMapping).isEqualTo(niMapping);
    }

    @Test
    void archetypeCompatibilityPresent() {
        var compat = asMap(manifest.get("archetypeCompatibility"));
        assertThat(compat).containsKeys("mbti", "enneagram", "disc", "belbin", "bigFive", "sdi");

        var mbti = asMap(compat.get("mbti"));
        assertThat(mbti).containsKey("intj");
        var intjFamilies = asList(mbti.get("intj"));
        assertThat(intjFamilies).contains("SAGE", "MAGICIAN", "SOVEREIGN");
    }

    @Test
    void archetypeAffinitiesPresent() {
        var affinities = asMap(manifest.get("archetypeAffinities"));
        assertThat(affinities).containsKey("angel");

        var angel = asMap(affinities.get("angel"));
        assertThat(angel).containsKeys("mbti", "enneagram");
        assertThat(asList(angel.get("mbti"))).contains("infj", "isfj");
    }

    @Test
    void impliesSupervisionFlagPresent() {
        var enneagram = asList(manifest.get("enneagram"));
        var type2 = enneagram.stream()
                .map(PersonalityVocabManifestTest::asMap)
                .filter(m -> m.get("value").equals("type-2"))
                .findFirst().orElseThrow();
        assertThat(type2).containsEntry("impliesSupervision", true);

        var type3 = enneagram.stream()
                .map(PersonalityVocabManifestTest::asMap)
                .filter(m -> m.get("value").equals("type-3"))
                .findFirst().orElseThrow();
        assertThat(type3).doesNotContainKey("impliesSupervision");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object obj) {
        return (Map<String, Object>) obj;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> asList(Object obj) {
        return (List<Object>) obj;
    }
}

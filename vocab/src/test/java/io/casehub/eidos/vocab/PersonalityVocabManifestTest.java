package io.casehub.eidos.vocab;

import io.casehub.eidos.api.DispositionAxis;
import io.casehub.eidos.api.VocabularyTerm;
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

    @Test
    void axisSignalWeightDefaultsToZero() {
        VocabularyTerm term = EnneagramTerm.TYPE_1;
        for (DispositionAxis axis : DispositionAxis.values()) {
            double weight = term.axisSignalWeight(axis);
            assertThat(weight).as("Weight for %s", axis)
                              .isBetween(-1.0, 1.0);
        }
    }

    @Test
    void dispositionAxesHavePoleDescriptions() {
        for (DispositionAxis axis : DispositionAxis.values()) {
            assertThat(axis.lowPoleDescription()).as(axis + " lowPoleDescription").isNotEmpty();
            assertThat(axis.highPoleDescription()).as(axis + " highPoleDescription").isNotEmpty();
        }
    }

    @Test
    void behavioralTendencyEnumHas22Constants() {
        assertThat(BehavioralTendency.values()).hasSize(22);
    }

    @Test
    void allTendenciesHaveNonEmptyConditions() {
        for (BehavioralTendency t : BehavioralTendency.values()) {
            assertThat(t.label()).as(t.name() + " label").isNotEmpty();
            assertThat(t.description()).as(t.name() + " description").isNotEmpty();
            assertThat(t.conditions()).as(t.name() + " conditions").isNotEmpty();
            for (BehavioralTendency.AxisCondition c : t.conditions()) {
                assertThat(c.axis()).as(t.name() + " condition axis").isNotNull();
                assertThat(c.direction()).as(t.name() + " condition direction").isNotNull();
                assertThat(c.threshold()).as(t.name() + " condition threshold").isGreaterThan(0);
            }
        }
    }

    @Test
    void mbtiTypesHaveSignalWeights() {
        for (MbtiTypeTerm t : MbtiTypeTerm.values()) {
            boolean hasNonZero = false;
            for (DispositionAxis axis : DispositionAxis.values()) {
                double w = t.axisSignalWeight(axis);
                assertThat(w).as(t + " " + axis).isBetween(-1.0, 1.0);
                if (w != 0.0) {hasNonZero = true;}
            }
            assertThat(hasNonZero).as(t + " has no signal weights").isTrue();
        }
    }

    @Test
    void mbtiTypesHaveAdjectives() {
        for (MbtiTypeTerm t : MbtiTypeTerm.values()) {
            assertThat(t.adjective()).as(t + " adjective").isNotEmpty();
        }
    }

    @Test
    void mbtiTypesHaveDetailedDescriptions() {
        for (MbtiTypeTerm t : MbtiTypeTerm.values()) {
            assertThat(t.detailedDescription()).as(t + " detailedDescription").isNotEmpty();
            assertThat(t.detailedDescription().length()).as(t + " detailedDescription length")
                                                        .isGreaterThan(100);
        }
    }

    @Test
    void allFrameworkTermsHaveSignalWeights() {
        assertAllHaveSignalWeights(EnneagramTerm.values());
        assertAllHaveSignalWeights(DiscTerm.values());
        assertAllHaveSignalWeights(BelbinTerm.values());
        assertAllHaveSignalWeights(SdiTerm.values());
        assertAllHaveSignalWeights(BigFiveTerm.values());
    }

    @Test
    void allFrameworkTermsHaveDetailedDescriptions() {
        for (EnneagramTerm t : EnneagramTerm.values()) {
            assertThat(t.detailedDescription()).as(t + " detailedDescription").isNotEmpty();
        }
        for (DiscTerm t : DiscTerm.values()) {
            assertThat(t.detailedDescription()).as(t + " detailedDescription").isNotEmpty();
        }
        for (BelbinTerm t : BelbinTerm.values()) {
            assertThat(t.detailedDescription()).as(t + " detailedDescription").isNotEmpty();
        }
        for (SdiTerm t : SdiTerm.values()) {
            assertThat(t.detailedDescription()).as(t + " detailedDescription").isNotEmpty();
        }
        for (BigFiveTerm t : BigFiveTerm.values()) {
            assertThat(t.detailedDescription()).as(t + " detailedDescription").isNotEmpty();
        }
    }

    @Test
    void enneagramDiscSdiHaveAdjectives() {
        for (EnneagramTerm t : EnneagramTerm.values()) {assertThat(t.adjective()).as(t + " adjective").isNotEmpty();}
        for (DiscTerm t : DiscTerm.values()) {assertThat(t.adjective()).as(t + " adjective").isNotEmpty();}
        for (SdiTerm t : SdiTerm.values()) {assertThat(t.adjective()).as(t + " adjective").isNotEmpty();}
    }

    private void assertAllHaveSignalWeights(VocabularyTerm[] terms) {
        for (VocabularyTerm t : terms) {
            boolean hasNonZero = false;
            for (DispositionAxis axis : DispositionAxis.values()) {
                double w = t.axisSignalWeight(axis);
                assertThat(w).as(t + " " + axis).isBetween(-1.0, 1.0);
                if (w != 0.0) {hasNonZero = true;}
            }
            assertThat(hasNonZero).as(t + " has no signal weights").isTrue();
        }
    }

    @Test
    void manifestContainsSignalWeights() {
        var mbtiTerms = asList(manifest.get("mbti"));
        var intj = mbtiTerms.stream().map(PersonalityVocabManifestTest::asMap)
                            .filter(m -> "intj".equals(m.get("value"))).findFirst().orElseThrow();
        assertThat(intj).containsKey("signalWeights");
        var weights = asMap(intj.get("signalWeights"));
        assertThat(((Number) weights.get("socialOrient")).doubleValue()).isCloseTo(-0.7, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    void manifestContainsBehavioralTendencies() {
        assertThat(manifest).containsKey("behavioralTendencies");
        var tendencies = asList(manifest.get("behavioralTendencies"));
        assertThat(tendencies).hasSize(22);
        var skeptical = asMap(tendencies.get(0));
        assertThat(skeptical).containsEntry("name", "Skeptical");
        assertThat(asList(skeptical.get("conditions"))).isNotEmpty();
    }

    @Test
    void manifestDispositionAxesHavePoleDescriptions() {
        var axes = asList(manifest.get("dispositionAxes"));
        for (var raw : axes) {
            var axis = asMap(raw);
            assertThat(axis).containsKey("lowPoleDescription");
            assertThat(axis).containsKey("highPoleDescription");
        }
    }

    @Test
    void manifestContainsFrameworkDescriptions() {
        assertThat(manifest).containsKey("frameworkDescriptions");
        var fwDescs = asMap(manifest.get("frameworkDescriptions"));
        assertThat(fwDescs).containsKeys("mbti", "enneagram", "disc", "belbin", "sdi", "bigFive");
    }

    @Test
    void manifestTermsHaveAdjectivesWhereSet() {
        var mbtiTerms = asList(manifest.get("mbti"));
        var intj = mbtiTerms.stream().map(PersonalityVocabManifestTest::asMap)
                            .filter(m -> "intj".equals(m.get("value"))).findFirst().orElseThrow();
        assertThat(intj).containsEntry("adjective", "analytical and strategic");
    }
}

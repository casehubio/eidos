package io.casehub.eidos.vocab;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.casehub.eidos.api.DispositionAxis;
import io.casehub.eidos.api.DispositionValue;
import io.casehub.eidos.api.VocabularyTerm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Build-time generator: extracts personality vocabulary metadata from Java enums
 * into {@code META-INF/eidos/personality-vocab.json}.
 *
 * <p>Run via exec-maven-plugin during prepare-package, or call {@link #generate()}
 * programmatically. The manifest is the single source of truth for frontends.
 */
public final class PersonalityVocabManifestGenerator {

    private PersonalityVocabManifestGenerator() {}

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: PersonalityVocabManifestGenerator <output-dir>");
            System.exit(1);
        }
        Path outputDir = Path.of(args[0], "META-INF", "eidos");
        Files.createDirectories(outputDir);
        Path outputFile = outputDir.resolve("personality-vocab.json");

        var mapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true);

        mapper.writeValue(outputFile.toFile(), generate());
        System.out.println("Generated: " + outputFile);
    }

    public static Map<String, Object> generate() {
        var manifest = new LinkedHashMap<String, Object>();

        manifest.put("dispositionAxes", buildDispositionAxes());
        manifest.put("axisValues", buildAxisValues());

        manifest.put("jungian", buildJungianTerms());
        manifest.put("mbti", buildMbtiTerms());
        manifest.put("enneagram", buildSimpleTerms(EnneagramTerm.values()));
        manifest.put("disc", buildSimpleTerms(DiscTerm.values()));
        manifest.put("belbin", buildSimpleTerms(BelbinTerm.values()));
        manifest.put("bigFive", buildSimpleTerms(BigFiveTerm.values()));
        manifest.put("sdi", buildSimpleTerms(SdiTerm.values()));

        manifest.put("archetype", buildArchetypeSection());

        manifest.put("axisMappings", buildAxisMappings());

        manifest.put("archetypeCompatibility", buildArchetypeCompatibility());
        manifest.put("archetypeAffinities", buildArchetypeAffinities());

        manifest.put("behavioralTendencies", buildBehavioralTendencies());
        manifest.put("frameworkDescriptions", buildFrameworkDescriptions());

        return manifest;
    }

    // ── Disposition axes ─────────────────────────────────────────

    private static List<Map<String, String>> buildDispositionAxes() {
        var result = new ArrayList<Map<String, String>>();
        for (var axis : DispositionAxis.values()) {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("key", axis.jsonKey());
            m.put("displayName", axis.displayName());
            m.put("description", axis.description());
            m.put("lowPole", axis.lowPoleLabel());
            m.put("highPole", axis.highPoleLabel());
            m.put("lowPoleDescription", axis.lowPoleDescription());
            m.put("highPoleDescription", axis.highPoleDescription());
            result.add(m);
        }
        return result;
    }

    // ── Axis value vocabularies ──────────────────────────────────

    private static Map<String, Object> buildAxisValues() {
        var result = new LinkedHashMap<String, Object>();
        result.put("conscientiousness", buildAxisVocab(
                ConscientiousnessTerm.URI, ConscientiousnessTerm.values()));
        result.put("thomasKilmann", buildAxisVocab(
                ThomasKilmannTerm.URI, ThomasKilmannTerm.values()));
        return result;
    }

    private static Map<String, Object> buildAxisVocab(String uri, VocabularyTerm[] terms) {
        var result = new LinkedHashMap<String, Object>();
        result.put("uri", uri);
        var termList = new ArrayList<Map<String, Object>>();
        for (var term : terms) {
            termList.add(termEntry(term));
        }
        result.put("terms", termList);
        return result;
    }

    // ── Jungian cognitive functions ──────────────────────────────

    private static List<Map<String, Object>> buildJungianTerms() {
        var result = new ArrayList<Map<String, Object>>();
        for (var fn : JungianFunctionTerm.values()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("value", fn.value());
            m.put("label", fn.label());
            m.put("description", fn.description());
            m.put("category", fn.category().name());
            m.put("attitude", fn.attitude().name());
            m.put("shadow", fn.shadow().value());
            m.put("responseStyleGuidance", fn.responseStyleGuidance());
            m.put("antiPatternWarning", fn.antiPatternWarning());
            result.add(m);
        }
        return result;
    }

    // ── MBTI types ──────────────────────────────────────────────

    private static List<Map<String, Object>> buildMbtiTerms() {
        var result = new ArrayList<Map<String, Object>>();
        for (var type : MbtiTypeTerm.values()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("value", type.value());
            m.put("label", type.label());
            m.put("description", type.description());

            List<VocabularyTerm> specs = type.specializes();
            m.put("dominant", specs.get(0).value());
            m.put("auxiliary", specs.get(1).value());

            Map<String, Double> profile = new LinkedHashMap<>();
            for (DispositionValue dv : type.defaultProfile()) {
                profile.put(dv.term(), dv.weight());
            }
            m.put("defaultProfile", profile);

            if (!type.detailedDescription().isEmpty()) {
                m.put("detailedDescription", type.detailedDescription());
            }
            if (!type.adjective().isEmpty()) {
                m.put("adjective", type.adjective());
            }
            var weights = buildSignalWeights(type);
            if (!weights.isEmpty()) {
                m.put("signalWeights", weights);
            }

            result.add(m);
        }
        return result;
    }

    // ── Simple personality vocabularies ──────────────────────────

    private static List<Map<String, Object>> buildSimpleTerms(VocabularyTerm[] terms) {
        var result = new ArrayList<Map<String, Object>>();
        for (var t : terms) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("value", t.value());
            m.put("label", t.label());
            m.put("description", t.description());
            if (!t.aliases().isEmpty()) {
                m.put("aliases", t.aliases());
            }
            if (t.impliesSupervision()) {
                m.put("impliesSupervision", true);
            }
            if (!t.detailedDescription().isEmpty()) {
                m.put("detailedDescription", t.detailedDescription());
            }
            if (!t.adjective().isEmpty()) {
                m.put("adjective", t.adjective());
            }
            var weights = buildSignalWeights(t);
            if (!weights.isEmpty()) {
                m.put("signalWeights", weights);
            }
            result.add(m);
        }
        return result;
    }

    // ── Archetype section ───────────────────────────────────────

    private static Map<String, Object> buildArchetypeSection() {
        var result = new LinkedHashMap<String, Object>();

        var families = new ArrayList<Map<String, String>>();
        for (var f : ArchetypeFamily.values()) {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("name", f.name());
            m.put("label", f.label());
            m.put("quadrant", f.quadrant().name());
            m.put("drive", f.drive());
            families.add(m);
        }
        result.put("families", families);

        var terms = new ArrayList<Map<String, Object>>();
        for (var t : ArchetypeTerm.values()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("value", t.value());
            m.put("label", t.label());
            m.put("description", t.description());
            m.put("family", t.family().name());
            m.put("validAdjectives", t.validAdjectives());
            m.put("invalidAdjectives", t.invalidAdjectives());
            terms.add(m);
        }
        result.put("terms", terms);

        return result;
    }

    // ── Axis mappings ───────────────────────────────────────────

    private static Map<String, Object> buildAxisMappings() {
        var result = new LinkedHashMap<String, Object>();

        result.put("jungian", buildAxisMap(JungianFunctionTerm.values()));
        result.put("mbti", buildMbtiAxisMap());
        result.put("enneagram", buildAxisMap(EnneagramTerm.values()));
        result.put("disc", buildAxisMap(DiscTerm.values()));
        result.put("belbin", buildAxisMap(BelbinTerm.values()));
        result.put("bigFive", buildAxisMap(BigFiveTerm.values()));
        result.put("sdi", buildAxisMap(SdiTerm.values()));

        return result;
    }

    private static Map<String, Map<String, String>> buildAxisMap(VocabularyTerm[] terms) {
        var result = new LinkedHashMap<String, Map<String, String>>();
        for (var term : terms) {
            var mapping = extractAxisMapping(term);
            if (!mapping.isEmpty()) {
                result.put(term.value(), mapping);
            }
        }
        return result;
    }

    private static Map<String, String> extractAxisMapping(VocabularyTerm term) {
        var mapping = new LinkedHashMap<String, String>();
        for (var axis : DispositionAxis.values()) {
            Optional<VocabularyTerm> match = term.axisExactMatch(
                    axis == DispositionAxis.CONFLICT_MODE
                            ? ThomasKilmannTerm.class
                            : ConscientiousnessTerm.class,
                    axis);
            match.ifPresent(vt -> mapping.put(axis.jsonKey(), vt.value()));
        }
        return mapping;
    }

    /**
     * MBTI types don't override axisExactMatch directly — their axis mappings
     * are derived from the dominant Jungian function.
     */
    private static Map<String, Map<String, String>> buildMbtiAxisMap() {
        var result = new LinkedHashMap<String, Map<String, String>>();
        for (var type : MbtiTypeTerm.values()) {
            var dominant = (JungianFunctionTerm) type.specializes().get(0);
            var mapping = extractAxisMapping(dominant);
            if (!mapping.isEmpty()) {
                result.put(type.value(), mapping);
            }
        }
        return result;
    }

    // ── Archetype compatibility ─────────────────────────────────

    private static Map<String, Object> buildArchetypeCompatibility() {
        var result = new LinkedHashMap<String, Object>();

        result.put("mbti", extractFamilyMap(MbtiTypeTerm.URI, MbtiTypeTerm.values()));
        result.put("enneagram", extractFamilyMap(EnneagramTerm.URI, EnneagramTerm.values()));
        result.put("disc", extractFamilyMap(DiscTerm.URI, DiscTerm.values()));
        result.put("belbin", extractFamilyMap(BelbinTerm.URI, BelbinTerm.values()));
        result.put("bigFive", extractFamilyMap(BigFiveTerm.URI, BigFiveTerm.values()));
        result.put("sdi", extractFamilyMap(SdiTerm.URI, SdiTerm.values()));

        return result;
    }

    private static Map<String, List<String>> extractFamilyMap(String uri, VocabularyTerm[] terms) {
        Set<ArchetypeFamily> allFamilies = Set.of(ArchetypeFamily.values());
        var result = new LinkedHashMap<String, List<String>>();
        for (var term : terms) {
            var families = ArchetypeCompatibility.compatibleFamilies(uri, term.value());
            if (!families.equals(allFamilies)) {
                result.put(term.value(), families.stream()
                        .sorted()
                        .map(ArchetypeFamily::name)
                        .toList());
            }
        }
        return result;
    }

    // ── Archetype affinities ────────────────────────────────────

    private static Map<String, Object> buildArchetypeAffinities() {
        String[] uris = { MbtiTypeTerm.URI, EnneagramTerm.URI };
        String[] keys = { "mbti", "enneagram" };
        VocabularyTerm[][] allTerms = {
                MbtiTypeTerm.values(), EnneagramTerm.values()
        };

        var result = new LinkedHashMap<String, Object>();
        for (var archetype : ArchetypeTerm.values()) {
            var affinities = new LinkedHashMap<String, List<String>>();
            for (int i = 0; i < uris.length; i++) {
                var matching = new ArrayList<String>();
                for (var term : allTerms[i]) {
                    if (ArchetypeCompatibility.subArchetypeAffinity(archetype, uris[i], term.value())) {
                        matching.add(term.value());
                    }
                }
                if (!matching.isEmpty()) {
                    affinities.put(keys[i], matching);
                }
            }
            if (!affinities.isEmpty()) {
                result.put(archetype.value(), affinities);
            }
        }
        return result;
    }

    // ── Shared helpers ──────────────────────────────────────────


    private static List<Map<String, Object>> buildBehavioralTendencies() {
        var result = new ArrayList<Map<String, Object>>();
        for (var t : BehavioralTendency.values()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", t.label());
            entry.put("description", t.description());
            var conditions = new ArrayList<Map<String, Object>>();
            for (var c : t.conditions()) {
                Map<String, Object> cond = new LinkedHashMap<>();
                cond.put("axis", c.axis().jsonKey());
                cond.put("direction", c.direction().name().toLowerCase());
                cond.put("threshold", c.threshold());
                conditions.add(cond);
            }
            entry.put("conditions", conditions);
            result.add(entry);
        }
        return result;
    }

    private static Map<String, Object> buildFrameworkDescriptions() {
        var result = new LinkedHashMap<String, Object>();
        result.put("mbti", Map.of(
                "description", "Myers-Briggs personality type — 16 types based on four cognitive preference pairs.",
                "detailedDescription", "The Myers-Briggs Type Indicator classifies personality along four axes: Introversion/Extraversion (energy source), Sensing/Intuition (information gathering), Thinking/Feeling (decision making), and Judging/Perceiving (lifestyle orientation). Each combination produces one of 16 types. In work contexts, MBTI helps predict communication style, decision-making approach, and team dynamics. The types describe preferences, not abilities — an INTJ can socialise and an ESFP can focus alone, but each draws energy differently."));
        result.put("enneagram", Map.of(
                "description", "Nine personality types based on core motivations and fears.",
                "detailedDescription", "The Enneagram describes nine interconnected personality types, each driven by a core motivation and a core fear. Unlike trait-based models, it focuses on why people behave as they do rather than how. Type 1 fears being corrupt, Type 5 fears being helpless, Type 8 fears being controlled. Each type has healthy, average, and unhealthy expressions. In professional settings, the Enneagram reveals what drives engagement, what triggers stress, and how people respond under pressure."));
        result.put("disc", Map.of(
                "description", "Four behavioural styles — Dominance, Influence, Steadiness, Conscientiousness.",
                "detailedDescription", "DISC measures observable behaviour across two axes: pace (fast vs. measured) and focus (task vs. people). D types are direct and results-driven. I types are enthusiastic and collaborative. S types are patient and supportive. C types are precise and analytical. Most people lead with one or two styles. DISC is widely used in workplace communication because it describes how people act rather than why — making it practical for adapting communication style to your audience."));
        result.put("belbin", Map.of(
                "description", "Nine team roles describing how people contribute to group work.",
                "detailedDescription", "Belbin Team Roles identify nine clusters of behaviour that emerge in team settings. Each person typically has a primary role (strongest contribution), one or two secondary roles, and roles they avoid. The nine roles span three categories: thinking roles (Plant, Monitor Evaluator, Specialist), action roles (Shaper, Implementer, Completer-Finisher), and people roles (Co-ordinator, Teamworker, Resource Investigator). Balanced teams need coverage across all three categories."));
        result.put("sdi", Map.of(
                "description", "Strength Deployment Inventory — motivational values driving behaviour.",
                "detailedDescription", "The SDI (Strength Deployment Inventory) maps three core motives: concern for people (Blue), concern for performance (Red), and concern for process (Green). Hub blends all three. Unlike behavioural models, SDI focuses on what people value and why they deploy their strengths. Crucially, SDI tracks how motives shift under conflict — a Blue (altruistic) person may shift to Red (assertive) when their values are threatened. This makes it particularly useful for understanding team friction."));
        result.put("bigFive", Map.of(
                "description", "Five broad personality dimensions backed by extensive research.",
                "detailedDescription", "The Big Five (OCEAN) is the most empirically validated personality model. It measures Openness to experience, Conscientiousness, Extraversion, Agreeableness, and Neuroticism (emotional reactivity). Each dimension is a spectrum, not a binary. High Openness correlates with creativity and adaptability; high Conscientiousness with reliability and follow-through; high Extraversion with energy in social settings; high Agreeableness with cooperation; low Neuroticism with emotional stability under pressure."));
        return result;
    }


    private static Map<String, Double> buildSignalWeights(VocabularyTerm term) {
        var weights = new LinkedHashMap<String, Double>();
        for (var axis : DispositionAxis.values()) {
            double w = term.axisSignalWeight(axis);
            if (w != 0.0) {
                weights.put(axis.jsonKey(), w);
            }
        }
        return weights;
    }

    private static Map<String, Object> termEntry(VocabularyTerm term) {
        var m = new LinkedHashMap<String, Object>();
        m.put("value", term.value());
        m.put("label", term.label());
        m.put("description", term.description());
        if (!term.aliases().isEmpty()) {
            m.put("aliases", term.aliases());
        }
        return m;
    }
}

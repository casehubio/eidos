package io.casehub.eidos.vocab;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ArchetypeCompatibility {

    private static final Map<String, Map<String, Set<ArchetypeFamily>>> FAMILY_MAP = new HashMap<>();
    private static final Map<ArchetypeTerm, Map<String, List<String>>> AFFINITY_MAP = new HashMap<>();
    private static final Set<ArchetypeFamily> ALL_FAMILIES = EnumSet.allOf(ArchetypeFamily.class);

    private static final Map<String, String> FRAMEWORK_URIS = Map.of(
        "mbti", MbtiTypeTerm.URI,
        "enneagram", EnneagramTerm.URI,
        "disc", DiscTerm.URI,
        "belbin", BelbinTerm.URI,
        "bigFive", BigFiveTerm.URI,
        "sdi", SdiTerm.URI
    );

    static {
        try {
            var yaml = new ObjectMapper(new YAMLFactory());
            var stream = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream("META-INF/eidos/archetypes.yaml");
            if (stream == null) throw new IllegalStateException("archetypes.yaml not found on classpath");
            JsonNode root;
            try (stream) {
                root = yaml.readTree(stream);
            }

            var compat = root.get("compatibility");
            compat.fields().forEachRemaining(framework -> {
                var uri = FRAMEWORK_URIS.get(framework.getKey());
                if (uri == null) return;
                var termMap = new HashMap<String, Set<ArchetypeFamily>>();
                framework.getValue().fields().forEachRemaining(entry -> {
                    var families = new java.util.HashSet<ArchetypeFamily>();
                    entry.getValue().forEach(f ->
                        families.add(ArchetypeFamily.valueOf(f.asText().toUpperCase(Locale.ROOT))));
                    termMap.put(entry.getKey().toLowerCase(Locale.ROOT), Set.copyOf(families));
                });
                FAMILY_MAP.put(uri, Map.copyOf(termMap));
            });

            var families = root.get("families");
            families.fields().forEachRemaining(family ->
                family.getValue().get("subArchetypes").fields().forEachRemaining(sub -> {
                    var term = ArchetypeTerm.valueOf(sub.getKey().toUpperCase(Locale.ROOT));
                    var affinities = sub.getValue().get("affinities");
                    if (affinities == null) return;
                    var uriMap = new HashMap<String, List<String>>();
                    affinities.fields().forEachRemaining(fw -> {
                        var fwUri = FRAMEWORK_URIS.get(fw.getKey());
                        if (fwUri == null) return;
                        var values = new ArrayList<String>();
                        fw.getValue().forEach(v -> {
                            var raw = v.asText().toLowerCase(Locale.ROOT);
                            if (fwUri.equals(EnneagramTerm.URI) && !raw.startsWith("type-")) {
                                raw = "type-" + raw;
                            }
                            values.add(raw);
                        });
                        uriMap.put(fwUri, List.copyOf(values));
                    });
                    AFFINITY_MAP.put(term, Map.copyOf(uriMap));
                })
            );
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private ArchetypeCompatibility() {}

    public static Set<ArchetypeFamily> compatibleFamilies(String frameworkUri, String termValue) {
        var framework = FAMILY_MAP.get(frameworkUri);
        if (framework == null) return ALL_FAMILIES;
        var families = framework.get(termValue.toLowerCase(Locale.ROOT));
        return families != null ? families : ALL_FAMILIES;
    }

    public static boolean subArchetypeAffinity(ArchetypeTerm archetype,
                                                String frameworkUri, String termValue) {
        var affinities = AFFINITY_MAP.get(archetype);
        if (affinities == null) return false;
        var values = affinities.get(frameworkUri);
        return values != null && values.contains(termValue.toLowerCase(Locale.ROOT));
    }

    public static String firstMbtiForFamily(ArchetypeFamily family) {
        var mbtiMap = FAMILY_MAP.get(MbtiTypeTerm.URI);
        if (mbtiMap == null) return null;
        for (var entry : mbtiMap.entrySet()) {
            if (entry.getValue().contains(family)) return entry.getKey();
        }
        return null;
    }

    public static List<String> mbtiAffinities(ArchetypeTerm term) {
        var affinities = AFFINITY_MAP.get(term);
        if (affinities == null) return List.of();
        var mbti = affinities.get(MbtiTypeTerm.URI);
        return mbti != null ? mbti : List.of();
    }
}

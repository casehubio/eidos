package io.casehub.eidos.vocab;

import io.casehub.eidos.api.AmpdFacetShift;
import io.casehub.eidos.api.SituationalProfile;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class SituationalProfileDefaults {

    private static final Map<ArchetypeFamily, List<SituationalProfile>> DEFAULTS;

    static {
        var yaml = new Yaml();
        InputStream is = SituationalProfileDefaults.class.getClassLoader()
                .getResourceAsStream("META-INF/eidos/archetype-situational-profiles.yaml");
        if (is == null) {
            throw new IllegalStateException("archetype-situational-profiles.yaml not found on classpath");
        }
        Map<String, Object> root = yaml.load(is);
        @SuppressWarnings("unchecked")
        Map<String, Object> families = (Map<String, Object>) root.get("families");

        var map = new EnumMap<ArchetypeFamily, List<SituationalProfile>>(ArchetypeFamily.class);
        for (var entry : families.entrySet()) {
            ArchetypeFamily family;
            try {
                family = ArchetypeFamily.valueOf(entry.getKey().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("Unknown family in situational profiles: " + entry.getKey());
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> modes = (Map<String, Object>) entry.getValue();
            var profiles = new ArrayList<SituationalProfile>();
            for (var modeEntry : modes.entrySet()) {
                String mode = modeEntry.getKey();
                @SuppressWarnings("unchecked")
                Map<String, Object> modeData = (Map<String, Object>) modeEntry.getValue();
                String provenance = (String) modeData.get("provenance");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> shiftList = (List<Map<String, Object>>) modeData.get("shifts");
                var shifts = new ArrayList<AmpdFacetShift>();
                for (var s : shiftList) {
                    shifts.add(new AmpdFacetShift((String) s.get("facet"), ((Number) s.get("delta")).intValue()));
                }
                profiles.add(new SituationalProfile(mode, shifts, provenance));
            }
            map.put(family, Collections.unmodifiableList(profiles));
        }
        DEFAULTS = Collections.unmodifiableMap(map);
    }

    private SituationalProfileDefaults() {}

    public static List<SituationalProfile> forFamily(ArchetypeFamily family) {
        return DEFAULTS.getOrDefault(family, List.of());
    }

    public static Map<ArchetypeFamily, List<SituationalProfile>> all() {
        return DEFAULTS;
    }
}

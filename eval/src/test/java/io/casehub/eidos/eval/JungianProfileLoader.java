package io.casehub.eidos.eval;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class JungianProfileLoader {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory())
        .findAndRegisterModules()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    List<JungianProfile> load() {
        final List<String> filenames = loadIndex();
        final List<JungianProfile> profiles = new ArrayList<>();
        for (final String filename : filenames) {
            profiles.add(loadProfile(filename));
        }
        return profiles;
    }

    @SuppressWarnings("unchecked")
    List<String> loadIndex() {
        try (InputStream is = cl().getResourceAsStream("jungian-profiles/index.yaml")) {
            if (is == null) throw new IllegalStateException("jungian-profiles/index.yaml not found");
            final Map<String, List<String>> index = YAML.readValue(is, Map.class);
            return index.get("profiles");
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to load jungian-profiles/index.yaml", e);
        }
    }

    private JungianProfile loadProfile(final String filename) {
        final String path = "jungian-profiles/" + filename;
        try (InputStream is = cl().getResourceAsStream(path)) {
            if (is == null) throw new IllegalStateException("Profile not found: " + path);
            return YAML.readValue(is, JungianProfile.class);
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to load profile: " + path, e);
        }
    }

    private ClassLoader cl() {
        return Thread.currentThread().getContextClassLoader();
    }
}

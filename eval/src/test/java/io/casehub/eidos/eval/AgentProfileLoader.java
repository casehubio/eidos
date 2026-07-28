package io.casehub.eidos.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.casehub.eidos.api.AgentDisposition;
import io.casehub.eidos.api.DispositionAxis;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

class AgentProfileLoader {

    private static final ObjectMapper YAML =
        new ObjectMapper(new YAMLFactory()).findAndRegisterModules();

    List<AgentProfile> load() {
        final VariantIndex index = loadIndex();
        final Map<String, AgentProfile> byName = new LinkedHashMap<>();
        for (final String filename : index.profiles()) {
            final AgentProfile p = loadProfile(filename);
            byName.put(p.name(), p);
        }
        validateVariantPairs(index, byName);
        return new ArrayList<>(byName.values());
    }

    VariantIndex loadIndex() {
        try (InputStream is = cl().getResourceAsStream("profiles/index.yaml")) {
            if (is == null) return new VariantIndex(List.of(), List.of());
            final VariantIndex raw = YAML.readValue(is, VariantIndex.class);
            final List<VariantPair> normalized = raw.variants().stream()
                .map(AgentProfileLoader::normalize)
                .toList();
            return new VariantIndex(raw.profiles(), normalized);
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to load profiles/index.yaml", e);
        }
    }

    static VariantPair normalize(final VariantPair v) {
        return new VariantPair(v.primaryAxis(), v.higher(), v.lower(),
            v.scenarioQuestions() != null ? v.scenarioQuestions() : List.of());
    }

    private AgentProfile loadProfile(final String filename) {
        final String path = "profiles/" + filename;
        try (InputStream is = cl().getResourceAsStream(path)) {
            if (is == null) throw new IllegalStateException("Profile not found: " + path);
            return YAML.readValue(is, AgentProfile.class);
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to load profile: " + path, e);
        }
    }

    private void validateVariantPairs(final VariantIndex index,
                                       final Map<String, AgentProfile> byName) {
        for (final VariantPair pair : index.variants()) {
            final AgentProfile hi = byName.get(pair.higher());
            final AgentProfile lo = byName.get(pair.lower());
            if (hi == null) throw new IllegalStateException(
                "Variant pair higher slug not found: " + pair.higher());
            if (lo == null) throw new IllegalStateException(
                "Variant pair lower slug not found: " + pair.lower());

            final AgentDisposition dh = hi.descriptor().disposition();
            final AgentDisposition dl = lo.descriptor().disposition();

            final String axHi = dh != null ? dh.primaryTerm(pair.primaryAxis()) : null;
            final String axLo = dl != null ? dl.primaryTerm(pair.primaryAxis()) : null;
            if (Objects.equals(axHi, axLo)) throw new IllegalStateException(
                "Pair " + pair.higher() + " vs " + pair.lower()
                + ": primaryAxis '" + pair.primaryAxis() + "' has same value: " + axHi);

            for (final DispositionAxis other : DispositionAxis.values()) {
                if (other != pair.primaryAxis()) {
                    final String dhVal = dh != null ? dh.primaryTerm(other) : null;
                    final String dlVal = dl != null ? dl.primaryTerm(other) : null;
                    if (!Objects.equals(dhVal, dlVal)) throw new IllegalStateException(
                        "Pair " + pair.higher() + " vs " + pair.lower()
                        + ": non-primary axis '" + other + "' differs");
                }
            }
            if ((dh != null && dh.delegation()) != (dl != null && dl.delegation()))
                throw new IllegalStateException(
                    "Pair " + pair.higher() + " vs " + pair.lower() + ": delegation differs");
        }
    }

    // package-private for testing
    void validatePairs(final VariantIndex index, final Map<String, AgentProfile> profiles) {
        validateVariantPairs(index, profiles);
    }

    private ClassLoader cl() {
        return Thread.currentThread().getContextClassLoader();
    }
}

package io.casehub.eidos.core.vocabulary;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.eidos.api.DataDrivenTerm;
import io.casehub.eidos.api.DataDrivenVocabulary;
import io.casehub.eidos.api.VocabularyTerm;
import io.casehub.eidos.api.spi.DataDrivenVocabularyRegistrar;
import io.casehub.yaml.jackson.YamlMappers;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;

public class ClasspathYamlVocabularyRegistrar implements DataDrivenVocabularyRegistrar {

    private static final String RESOURCE_PATH = "META-INF/eidos/vocabularies.yaml";
    private static final ObjectMapper YAML_MAPPER = YamlMappers.create()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

    @Override
    public List<DataDrivenVocabulary> vocabularies() {
        final Enumeration<URL> urls;
        try {
            urls = Thread.currentThread().getContextClassLoader().getResources(RESOURCE_PATH);
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to scan classpath for " + RESOURCE_PATH, e);
        }

        final var all = new ArrayList<DataDrivenVocabulary>();
        while (urls.hasMoreElements()) {
            final var url = urls.nextElement();
            try (final var stream = url.openStream()) {
                all.add(loadFrom(stream));
            } catch (final Exception e) {
                throw new IllegalStateException(
                    "Failed to load vocabulary from " + url + ": " + e.getMessage(), e);
            }
        }
        return List.copyOf(all);
    }

    public DataDrivenVocabulary loadFrom(final InputStream yaml) {
        if (yaml == null) throw new IllegalArgumentException("yaml stream must not be null");
        final VocabularyYaml raw;
        try {
            raw = YAML_MAPPER.readValue(yaml, VocabularyYaml.class);
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to parse vocabulary YAML: " + e.getMessage(), e);
        }
        return resolveAndBuild(raw);
    }

    private DataDrivenVocabulary resolveAndBuild(final VocabularyYaml raw) {
        final var rawTerms = raw.terms != null ? raw.terms : List.<TermYaml>of();

        // First pass: create terms without resolved parents, index by value
        final var termsByValue = new LinkedHashMap<String, DataDrivenTerm>();
        for (final var t : rawTerms) {
            var term = new DataDrivenTerm(t.value, t.label, t.description,
                t.aliases != null ? t.aliases : List.of(),
                t.keywords != null ? t.keywords : List.of(),
                List.of());
            termsByValue.put(t.value, term);
        }

        // Second pass: resolve specializes references and rebuild with parents.
        // Update termsByValue with each resolved term so children see the fully-resolved parent
        // (records use value-based equals; without this, 3+-level hierarchies break in the
        // registry's termToUri lookup because first-pass and second-pass objects are not equal).
        final var resolved = new ArrayList<DataDrivenTerm>();
        for (final var t : rawTerms) {
            List<VocabularyTerm> parents = List.of();
            if (t.specializes != null && !t.specializes.isEmpty()) {
                final var parentList = new ArrayList<VocabularyTerm>();
                for (final var parentValue : t.specializes) {
                    var parent = termsByValue.get(parentValue);
                    if (parent == null) {
                        throw new IllegalArgumentException(
                            "Unresolvable specializes reference '" + parentValue
                            + "' in term '" + t.value + "' of vocabulary '" + raw.uri + "'");
                    }
                    parentList.add(parent);
                }
                parents = List.copyOf(parentList);
            }
            var resolvedTerm = new DataDrivenTerm(t.value, t.label, t.description,
                t.aliases != null ? t.aliases : List.of(),
                t.keywords != null ? t.keywords : List.of(),
                parents);
            resolved.add(resolvedTerm);
            termsByValue.put(t.value, resolvedTerm);
        }

        return new DataDrivenVocabulary(raw.uri, raw.name, raw.version, raw.description, resolved);
    }

    static class VocabularyYaml {
        public String uri, name, version, description;
        public List<TermYaml> terms;
    }

    static class TermYaml {
        public String value, label, description;
        public List<String> aliases, keywords, specializes;
    }
}

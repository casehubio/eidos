package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class DomainKeywordsTest {
    @Test void returns_own_keywords_when_no_ancestors() {
        var term = new DataDrivenTerm("java", "Java", "", List.of(),
            List.of("jvm", "bytecode"), List.of());
        var registry = stubRegistry("urn:test", term);
        assertThat(DomainKeywords.expanded(registry, "urn:test", "java"))
            .containsExactly("jvm", "bytecode");
    }

    @Test void includes_ancestor_keywords() {
        var parent = new DataDrivenTerm("se", "SE", "", List.of(),
            List.of("code", "debug"), List.of());
        var child = new DataDrivenTerm("java", "Java", "", List.of(),
            List.of("jvm"), List.of(parent));
        var registry = stubRegistry("urn:test", parent, child);
        assertThat(DomainKeywords.expanded(registry, "urn:test", "java"))
            .containsExactly("jvm", "code", "debug");
    }

    @Test void deduplicates_keywords() {
        var parent = new DataDrivenTerm("se", "SE", "", List.of(),
            List.of("code", "jvm"), List.of());
        var child = new DataDrivenTerm("java", "Java", "", List.of(),
            List.of("jvm", "bytecode"), List.of(parent));
        var registry = stubRegistry("urn:test", parent, child);
        var result = DomainKeywords.expanded(registry, "urn:test", "java");
        assertThat(result).containsExactly("jvm", "bytecode", "code");
    }

    @Test void returns_empty_for_unknown_term() {
        var registry = stubRegistry("urn:test",
            new DataDrivenTerm("a", "A", "", List.of(), List.of("x"), List.of()));
        assertThat(DomainKeywords.expanded(registry, "urn:test", "unknown")).isEmpty();
    }

    @Test void returns_empty_for_unknown_vocabulary() {
        var registry = stubRegistry("urn:test",
            new DataDrivenTerm("a", "A", "", List.of(), List.of("x"), List.of()));
        assertThat(DomainKeywords.expanded(registry, "urn:other", "a")).isEmpty();
    }

    private VocabularyRegistry stubRegistry(String uri, DataDrivenTerm... terms) {
        var termMap = new LinkedHashMap<String, VocabularyTerm>();
        for (var t : terms) termMap.put(t.value(), t);
        return new MinimalTestRegistry(uri, termMap);
    }

    private static class MinimalTestRegistry implements VocabularyRegistry {
        private final String uri;
        private final Map<String, VocabularyTerm> terms;

        MinimalTestRegistry(String uri, Map<String, VocabularyTerm> terms) {
            this.uri = uri;
            this.terms = terms;
        }

        @Override public Optional<? extends VocabularyTerm> resolve(String vocabUri, String value) {
            if (!uri.equals(vocabUri)) return Optional.empty();
            return Optional.ofNullable(terms.get(value));
        }

        @Override public List<? extends VocabularyTerm> ancestors(String vocabUri, String value) {
            if (!uri.equals(vocabUri)) return List.of();
            var term = terms.get(value);
            if (term == null) return List.of();
            var result = new ArrayList<VocabularyTerm>();
            var current = term;
            while (!current.specializes().isEmpty()) {
                current = current.specializes().get(0);
                result.add(current);
            }
            return result;
        }

        @Override public <T extends Enum<T> & VocabularyTerm> void register(Class<T> vocab) { throw new UnsupportedOperationException(); }
        @Override public void register(String uri, String name, String version, String description, List<? extends VocabularyTerm> terms) { throw new UnsupportedOperationException(); }
        @Override public boolean isRegistered(String vocabUri) { return uri.equals(vocabUri); }
        @Override public Set<String> registeredUris() { return Set.of(uri); }
        @Override public List<? extends VocabularyTerm> allTerms(String vocabUri) { return List.of(); }
        @Override public Optional<String> equivalentValues(String fromUri, String value, String toUri) { return Optional.empty(); }
        @Override public Optional<String> equivalentValues(String fromUri, String value, String toUri, DispositionAxis axis) { return Optional.empty(); }
        @Override public <T extends Enum<T> & VocabularyTerm> Optional<T> resolve(Class<T> vocab, String value) { return Optional.empty(); }
        @Override public <S extends Enum<S> & VocabularyTerm, T extends Enum<T> & VocabularyTerm> Optional<T> equivalentValues(S from, Class<T> targetVocab) { return Optional.empty(); }
        @Override public <S extends Enum<S> & VocabularyTerm, T extends Enum<T> & VocabularyTerm> Optional<T> equivalentValues(S from, Class<T> targetVocab, DispositionAxis axis) { return Optional.empty(); }
        @Override public Optional<VocabularyInfo> vocabularyMetadata(String uri) { return Optional.empty(); }
        @Override public boolean subsumes(String vocabUri, String generalValue, String specificValue) { return false; }
        @Override public MatchDegree match(String vocabUri, String declaredValue, String requestedValue) { return new MatchDegree.None(); }
        @Override public List<? extends VocabularyTerm> descendants(String vocabUri, String value) { return List.of(); }
        @Override public Map<String, Set<String>> expandForMatchingByVocabulary(String value) { return Map.of(); }
    }
}

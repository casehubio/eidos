package io.casehub.eidos.vocab;

import io.casehub.eidos.api.*;
import io.casehub.eidos.core.vocabulary.ClasspathYamlVocabularyRegistrar;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class EpistemicDomainVocabTest {

    static final String                 URI = "urn:casehub:vocab:epistemic-domain";
    static       TestDataDrivenRegistry registry;

    @BeforeAll
    static void setup() {
        registry = new TestDataDrivenRegistry();
        var registrar = new ClasspathYamlVocabularyRegistrar();
        for (var vocab : registrar.vocabularies()) {
            registry.registerVocabulary(vocab);
        }
    }

    @Test
    void vocabulary_is_registered() {
        assertThat(registry.isRegistered(URI)).isTrue();
    }

    @Test
    void has_approximately_20_terms() {
        var terms = registry.allTerms(URI);
        assertThat(terms).hasSizeGreaterThanOrEqualTo(18);
        assertThat(terms).hasSizeLessThanOrEqualTo(25);
    }

    @Test
    void java_specializes_software_engineering() {
        assertThat(registry.subsumes(URI, "software-engineering", "java")).isTrue();
    }

    @Test
    void java_has_keywords() {
        var term = registry.resolve(URI, "java");
        assertThat(term).isPresent();
        assertThat(term.get().keywords()).contains("jvm", "bytecode");
    }

    @Test
    void expanded_keywords_include_ancestors() {
        var expanded = DomainKeywords.expanded(registry, URI, "java");
        assertThat(expanded).contains("jvm");
        assertThat(expanded).contains("code");
    }

    @Test
    void owlsmx_matching_works() {
        var match = registry.match(URI, "software-engineering", "java");
        assertThat(match).isInstanceOf(MatchDegree.Plugin.class);
    }

    @Test
    void static_analysis_specializes_analysis() {
        assertThat(registry.subsumes(URI, "analysis", "static-analysis")).isTrue();
    }

    @Test
    void top_level_domains_have_no_parents() {
        assertThat(registry.ancestors(URI, "software-engineering")).isEmpty();
        assertThat(registry.ancestors(URI, "analysis")).isEmpty();
        assertThat(registry.ancestors(URI, "security")).isEmpty();
    }

    @Test
    void vocabulary_metadata_populated() {
        var info = registry.vocabularyMetadata(URI);
        assertThat(info).isPresent();
        assertThat(info.get().name()).isEqualTo("Epistemic Domain Taxonomy");
    }

    @Test
    void application_security_has_keywords() {
        var term = registry.resolve(URI, "application-security");
        assertThat(term).isPresent();
        assertThat(term.get().keywords()).contains("owasp", "injection");
    }

    @Test
    void descendants_of_software_engineering() {
        var descendants = registry.descendants(URI, "software-engineering");
        assertThat(descendants).extracting(VocabularyTerm::value)
                               .contains("java", "python", "rust", "go", "typescript");
    }

    /**
     * Minimal VocabularyRegistry for testing data-driven vocabularies without runtime dependency.
     * Supports registration, resolution, hierarchy, and matching.
     */
    private static class TestDataDrivenRegistry implements VocabularyRegistry {
        private final Map<String, Map<String, VocabularyTerm>>    termsByUri    = new LinkedHashMap<>();
        private final Map<String, List<? extends VocabularyTerm>> orderedByUri  = new LinkedHashMap<>();
        private final Map<String, VocabularyInfo>                 metadataByUri = new LinkedHashMap<>();

        void registerVocabulary(DataDrivenVocabulary vocab) {
            var lookup = new LinkedHashMap<String, VocabularyTerm>();
            for (var term : vocab.terms()) {
                lookup.put(term.value(), term);
                for (var alias : term.aliases()) {lookup.put(alias, term);}
            }
            termsByUri.put(vocab.uri(), Map.copyOf(lookup));
            orderedByUri.put(vocab.uri(), List.copyOf(vocab.terms()));
            metadataByUri.put(vocab.uri(), new VocabularyInfo(vocab.uri(), vocab.name(), vocab.version(), vocab.description()));
        }

        @Override
        public Optional<? extends VocabularyTerm> resolve(String vocabUri, String value) {
            var m = termsByUri.get(vocabUri);
            return m == null ? Optional.empty() : Optional.ofNullable(m.get(value));
        }

        @Override
        public List<? extends VocabularyTerm> allTerms(String vocabUri) {
            return orderedByUri.getOrDefault(vocabUri, List.of());
        }

        @Override
        public boolean isRegistered(String vocabUri)                   {return metadataByUri.containsKey(vocabUri);}

        @Override
        public Set<String> registeredUris()                            {return Set.copyOf(metadataByUri.keySet());}

        @Override
        public Optional<VocabularyInfo> vocabularyMetadata(String uri) {return Optional.ofNullable(metadataByUri.get(uri));}

        @Override
        public List<? extends VocabularyTerm> ancestors(String vocabUri, String value) {
            var m = termsByUri.get(vocabUri);
            if (m == null) {return List.of();}
            var term = m.get(value);
            if (term == null) {return List.of();}
            var result  = new ArrayList<VocabularyTerm>();
            var current = term;
            while (!current.specializes().isEmpty()) {
                current = current.specializes().get(0);
                result.add(current);
            }
            return result;
        }

        @Override
        public List<? extends VocabularyTerm> descendants(String vocabUri, String value) {
            var terms = orderedByUri.get(vocabUri);
            if (terms == null) {return List.of();}
            return terms.stream()
                        .filter(t -> ancestors(vocabUri, t.value()).stream().anyMatch(a -> a.value().equals(value)))
                        .map(t -> (VocabularyTerm) t)
                        .toList();
        }

        @Override
        public boolean subsumes(String vocabUri, String generalValue, String specificValue) {
            if (generalValue.equals(specificValue)) {return true;}
            return ancestors(vocabUri, specificValue).stream().anyMatch(a -> a.value().equals(generalValue));
        }

        @Override
        public MatchDegree match(String vocabUri, String declaredValue, String requestedValue) {
            if (declaredValue.equals(requestedValue)) {return new MatchDegree.Exact();}
            var reqAncestors = ancestors(vocabUri, requestedValue);
            for (int i = 0; i < reqAncestors.size(); i++) {
                if (reqAncestors.get(i).value().equals(declaredValue)) {return new MatchDegree.Plugin(i + 1);}
            }
            var declAncestors = ancestors(vocabUri, declaredValue);
            for (int i = 0; i < declAncestors.size(); i++) {
                if (declAncestors.get(i).value().equals(requestedValue)) {return new MatchDegree.Specialization(i + 1);}
            }
            return new MatchDegree.None();
        }

        @Override
        public <T extends Enum<T> & VocabularyTerm> void register(Class<T> vocab)                                                                                        {throw new UnsupportedOperationException();}

        @Override
        public void register(String uri, String name, String version, String description, List<? extends VocabularyTerm> terms)                                          {throw new UnsupportedOperationException();}

        @Override
        public Optional<String> equivalentValues(String fromUri, String value, String toUri)                                                                             {return Optional.empty();}

        @Override
        public Optional<String> equivalentValues(String fromUri, String value, String toUri, DispositionAxis axis)                                                       {return Optional.empty();}

        @Override
        public <T extends Enum<T> & VocabularyTerm> Optional<T> resolve(Class<T> vocab, String value)                                                                    {return Optional.empty();}

        @Override
        public <S extends Enum<S> & VocabularyTerm, T extends Enum<T> & VocabularyTerm> Optional<T> equivalentValues(S from, Class<T> targetVocab)                       {return Optional.empty();}

        @Override
        public <S extends Enum<S> & VocabularyTerm, T extends Enum<T> & VocabularyTerm> Optional<T> equivalentValues(S from, Class<T> targetVocab, DispositionAxis axis) {return Optional.empty();}

        @Override
        public Map<String, Set<String>> expandForMatchingByVocabulary(String value)                                                                                      {return Map.of();}
    }
}

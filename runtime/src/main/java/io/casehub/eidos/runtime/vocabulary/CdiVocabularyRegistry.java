package io.casehub.eidos.runtime.vocabulary;

import io.casehub.eidos.api.*;
import io.quarkus.arc.DefaultBean;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@DefaultBean
@ApplicationScoped
public class CdiVocabularyRegistry implements VocabularyRegistry {

    @Inject @Any Instance<Vocabulary> cdiBeans;

    private final ConcurrentHashMap<String, Vocabulary> registry = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        for (Vocabulary v : cdiBeans) {
            registry.put(v.uri(), v);
        }
    }

    @Override
    public void register(Vocabulary vocabulary) {
        registry.put(vocabulary.uri(), vocabulary);
    }

    @Override
    public Optional<Vocabulary> find(String uri) {
        return Optional.ofNullable(registry.get(uri));
    }

    @Override
    public Optional<VocabularyTerm> resolve(String vocabularyUri, String value) {
        return find(vocabularyUri)
            .flatMap(vocab -> vocab.terms().values().stream()
                .filter(t -> t.value().equals(value) || t.aliases().contains(value))
                .findFirst());
    }

    /**
     * Returns all exact-match values in {@code targetVocabularyUri} reachable from any term
     * in {@code vocabularyUri} that matches {@code value} by primary value or alias.
     *
     * <p>Collects across <em>all</em> matching terms — not just the first. If multiple terms
     * share the same value or alias, and each carries an exact-match entry for
     * {@code targetVocabularyUri}, all mapped values are returned in the result set.
     *
     * @return exact-match values in the target vocabulary; empty set if no match or unknown URIs
     */
    @Override
    public Set<String> equivalentValues(String vocabularyUri, String value, String targetVocabularyUri) {
        return find(vocabularyUri)
            .map(vocab -> vocab.terms().values().stream()
                .filter(t -> t.value().equals(value) || t.aliases().contains(value))
                .map(t -> t.exactMatches().get(targetVocabularyUri))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()))
            .orElse(Set.of());
    }
}

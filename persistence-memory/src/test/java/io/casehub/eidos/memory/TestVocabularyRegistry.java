package io.casehub.eidos.memory;

import io.casehub.eidos.api.DispositionAxis;
import io.casehub.eidos.api.MatchDegree;
import io.casehub.eidos.api.VocabularyMetadata;
import io.casehub.eidos.api.VocabularyRegistry;
import io.casehub.eidos.api.VocabularyTerm;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Minimal VocabularyRegistry implementation for testing InMemoryAgentRegistry.
 * Supports registration, resolution, and subsumption matching.
 */
@ApplicationScoped
public class TestVocabularyRegistry implements VocabularyRegistry {

    private final Map<String, Class<? extends Enum<? extends VocabularyTerm>>> byUri = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<? extends VocabularyTerm>> byClass = new ConcurrentHashMap<>();

    @Override
    public <T extends Enum<T> & VocabularyTerm> void register(Class<T> vocab) {
        var metadata = vocab.getAnnotation(VocabularyMetadata.class);
        if (metadata == null) {
            throw new IllegalArgumentException("Vocabulary " + vocab.getName() + " missing @VocabularyMetadata");
        }
        String uri = metadata.uri();
        if (uri == null || uri.isBlank()) {
            throw new IllegalArgumentException("Vocabulary URI cannot be blank");
        }

        var constants = Arrays.asList(vocab.getEnumConstants());
        if (constants.isEmpty()) {
            throw new IllegalArgumentException("Vocabulary " + vocab.getName() + " has no constants");
        }

        byUri.put(uri, vocab);
        byClass.put(vocab, constants);
    }

    @Override
    public boolean isRegistered(String vocabUri) {
        return byUri.containsKey(vocabUri);
    }

    @Override
    public Set<String> registeredUris() {
        return Set.copyOf(byUri.keySet());
    }


    @Override
    public Optional<? extends VocabularyTerm> resolve(String vocabUri, String value) {
        var vocab = byUri.get(vocabUri);
        if (vocab == null) {
            return Optional.empty();
        }
        var terms = byClass.get(vocab);
        return terms.stream()
            .filter(t -> t.value().equals(value) || t.aliases().contains(value))
            .findFirst();
    }

    @Override
    public List<? extends VocabularyTerm> allTerms(String vocabUri) {
        var vocab = byUri.get(vocabUri);
        return vocab != null ? byClass.get(vocab) : List.of();
    }

    @Override
    public Optional<String> equivalentValues(String fromUri, String value, String toUri) {
        return Optional.empty(); // Not needed for registry tests
    }

    @Override
    public Optional<String> equivalentValues(String fromUri, String value, String toUri, DispositionAxis axis) {
        return Optional.empty(); // Not needed for registry tests
    }

    @Override
    public <T extends Enum<T> & VocabularyTerm> Optional<T> resolve(Class<T> vocab, String value) {
        var terms = byClass.get(vocab);
        if (terms == null) {
            return Optional.empty();
        }
        return terms.stream()
            .filter(t -> t.value().equals(value) || t.aliases().contains(value))
            .map(vocab::cast)
            .findFirst();
    }

    @Override
    public <S extends Enum<S> & VocabularyTerm, T extends Enum<T> & VocabularyTerm>
    Optional<T> equivalentValues(S from, Class<T> targetVocab) {
        return Optional.empty(); // Not needed for registry tests
    }

    @Override
    public <S extends Enum<S> & VocabularyTerm, T extends Enum<T> & VocabularyTerm>
    Optional<T> equivalentValues(S from, Class<T> targetVocab, DispositionAxis axis) {
        return Optional.empty(); // Not needed for registry tests
    }

    @Override
    public Optional<VocabularyMetadata> vocabularyMetadata(String uri) {
        var vocab = byUri.get(uri);
        return vocab != null ? Optional.of(vocab.getAnnotation(VocabularyMetadata.class)) : Optional.empty();
    }

    @Override
    public boolean subsumes(String vocabUri, String generalValue, String specificValue) {
        return !(match(vocabUri, generalValue, specificValue) instanceof MatchDegree.None);
    }

    @Override
    public MatchDegree match(String vocabUri, String declaredValue, String requestedValue) {
        if (declaredValue.equals(requestedValue)) {
            return new MatchDegree.Exact();
        }

        var declaredTerm = resolve(vocabUri, declaredValue);
        var requestedTerm = resolve(vocabUri, requestedValue);

        if (declaredTerm.isEmpty() || requestedTerm.isEmpty()) {
            return new MatchDegree.None();
        }

        // Check if requestedTerm specializes declaredTerm (by walking up the specialization chain)
        var current = requestedTerm.get();
        int depth = 0;
        while (!current.specializes().isEmpty()) {
            depth++;
            current = current.specializes().get(0); // Simple linear hierarchy for tests
            if (current.value().equals(declaredValue)) {
                return new MatchDegree.Specialization(depth);
            }
        }

        return new MatchDegree.None();
    }

    @Override
    public List<? extends VocabularyTerm> ancestors(String vocabUri, String value) {
        var term = resolve(vocabUri, value);
        if (term.isEmpty()) {
            return List.of();
        }

        List<VocabularyTerm> ancestors = new ArrayList<>();
        var current = term.get();
        while (!current.specializes().isEmpty()) {
            current = current.specializes().get(0);
            ancestors.add(current);
        }
        return ancestors;
    }

    @Override
    public List<? extends VocabularyTerm> descendants(String vocabUri, String value) {
        var vocab = byUri.get(vocabUri);
        if (vocab == null) {
            return List.of();
        }

        var allTerms = byClass.get(vocab);
        return allTerms.stream()
            .filter(t -> ancestors(vocabUri, t.value()).stream()
                .anyMatch(a -> a.value().equals(value)))
            .collect(Collectors.toList());
    }

    @Override
    public Map<String, Set<String>> expandForMatchingByVocabulary(String value) {
        Map<String, Set<String>> result = new HashMap<>();

        for (var entry : byUri.entrySet()) {
            String uri = entry.getKey();
            var term = resolve(uri, value);

            if (term.isPresent()) {
                // Found a term with this value - collect all descendants
                var descs = descendants(uri, value);
                if (!descs.isEmpty()) {
                    result.put(uri, descs.stream()
                        .map(VocabularyTerm::value)
                        .collect(Collectors.toSet()));
                }
            }
        }

        return result;
    }
}

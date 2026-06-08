package io.casehub.eidos.runtime.vocabulary;

import io.casehub.eidos.api.DispositionAxis;
import io.casehub.eidos.api.VocabularyMetadata;
import io.casehub.eidos.api.VocabularyRegistry;
import io.casehub.eidos.api.VocabularyTerm;
import io.casehub.eidos.api.spi.VocabularyRegistrar;
import io.quarkus.arc.DefaultBean;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CDI-discovered vocabulary registry.
 *
 * <p>Thread safety: {@code register()} is single-threaded — designed for {@code @PostConstruct}
 * initialization. The internal maps are {@code ConcurrentHashMap} for safe concurrent reads
 * after {@code @PostConstruct} completes.
 */
@DefaultBean
@ApplicationScoped
public class CdiVocabularyRegistry implements VocabularyRegistry {

    @Inject @Any Instance<VocabularyRegistrar> registrars;

    // vocabUri → enum class
    private final ConcurrentHashMap<String, Class<? extends Enum<?>>> byUri =
        new ConcurrentHashMap<>();
    // class → (value + aliases) → constant  (lookup index)
    private final ConcurrentHashMap<Class<?>, Map<String, VocabularyTerm>> byClass =
        new ConcurrentHashMap<>();
    // class → constants in declaration order  (for allTerms — stored immutably)
    private final ConcurrentHashMap<Class<?>, List<? extends VocabularyTerm>> byClassOrdered =
        new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        for (VocabularyRegistrar r : registrars) {
            registerRaw(r.vocabulary());
        }
    }

    /**
     * Bridges the wildcard return type of {@link VocabularyRegistrar#vocabulary()} to the
     * recursive bound required by {@link #register(Class)}. The cast is safe because any
     * concrete enum returned by the registrar satisfies {@code T extends Enum<T> & VocabularyTerm}
     * at its declaration site — the wildcard just loses that information at the use site.
     */
    @SuppressWarnings("unchecked")
    private <T extends Enum<T> & VocabularyTerm>
    void registerRaw(Class<? extends Enum<? extends VocabularyTerm>> vocab) {
        register((Class<T>) vocab);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Enum<T> & VocabularyTerm> void register(Class<T> vocab) {
        var meta = vocab.getAnnotation(VocabularyMetadata.class);
        if (meta == null) {
            throw new IllegalArgumentException(
                "Vocabulary enum " + vocab.getName() + " is missing @VocabularyMetadata");
        }
        var constants = vocab.getEnumConstants();
        if (constants == null || constants.length == 0) {
            throw new IllegalArgumentException(
                "Vocabulary enum " + vocab.getName() + " has no constants");
        }
        var uri = meta.uri();
        if (uri.isBlank()) {
            throw new IllegalArgumentException(
                "Vocabulary URI must not be blank in @VocabularyMetadata on " + vocab.getName());
        }
        // Validate URI and state before any map operations (fast-fail)
        var existing = byUri.get(uri);
        if (existing != null) {
            if (existing != vocab) {
                throw new IllegalArgumentException("URI " + uri + " already registered by "
                    + existing.getName() + "; cannot register " + vocab.getName());
            }
            return;  // same class already registered — idempotent, nothing to do
        }

        // Build ordered list locally — List.copyOf is immutable so allTerms() returns directly
        var orderedList = List.copyOf(Arrays.asList(constants));

        // Build lookup map locally (validates duplicates before writing)
        var lookupMap = new LinkedHashMap<String, VocabularyTerm>();
        for (var constant : constants) {
            if (lookupMap.containsKey(constant.value())) {
                throw new IllegalArgumentException(
                    "Duplicate primary value '" + constant.value() + "' in " + vocab.getName());
            }
            lookupMap.put(constant.value(), constant);
            for (var alias : constant.aliases()) {
                if (lookupMap.containsKey(alias)) {
                    throw new IllegalArgumentException(
                        "Alias '" + alias + "' conflicts with an existing value or alias in "
                            + vocab.getName());
                }
                lookupMap.put(alias, constant);
            }
        }

        // Write all three maps
        byClassOrdered.put(vocab, orderedList);
        byClass.put(vocab, Map.copyOf(lookupMap));
        byUri.put(uri, (Class<? extends Enum<?>>) vocab);
    }

    @Override
    public boolean isRegistered(String vocabUri) {
        return byUri.containsKey(vocabUri);
    }

    @Override
    public Optional<? extends VocabularyTerm> resolve(String vocabUri, String value) {
        var clazz = byUri.get(vocabUri);
        if (clazz == null) return Optional.empty();
        return Optional.ofNullable(byClass.get(clazz).get(value));
    }

    @Override
    public List<? extends VocabularyTerm> allTerms(String vocabUri) {
        var clazz = byUri.get(vocabUri);
        return clazz == null ? List.of() : byClassOrdered.get(clazz);
    }

    @Override
    public Optional<String> equivalentValues(String fromUri, String value, String toUri) {
        var sourceClass = byUri.get(fromUri);
        if (sourceClass == null) return Optional.empty();
        var term = byClass.get(sourceClass).get(value);
        if (term == null) return Optional.empty();
        var targetClass = byUri.get(toUri);
        if (targetClass == null) return Optional.empty();
        return term.exactMatch(targetClass).map(VocabularyTerm::value);
    }

    @Override
    public Optional<String> equivalentValues(String fromUri, String value, String toUri,
                                              DispositionAxis axis) {
        var sourceClass = byUri.get(fromUri);
        if (sourceClass == null) return Optional.empty();
        var term = byClass.get(sourceClass).get(value);
        if (term == null) return Optional.empty();
        var targetClass = byUri.get(toUri);
        if (targetClass == null) return Optional.empty();
        return term.axisExactMatch(targetClass, axis).map(VocabularyTerm::value);
    }

    @Override
    public <T extends Enum<T> & VocabularyTerm> Optional<T> resolve(Class<T> vocab, String value) {
        var lookup = byClass.get(vocab);
        return lookup == null
            ? Optional.empty()
            : Optional.ofNullable(vocab.cast(lookup.get(value)));
    }

    @Override
    public <S extends Enum<S> & VocabularyTerm, T extends Enum<T> & VocabularyTerm>
    Optional<T> equivalentValues(S from, Class<T> targetVocab) {
        return from.exactMatch(targetVocab).map(targetVocab::cast);
    }

    @Override
    public <S extends Enum<S> & VocabularyTerm, T extends Enum<T> & VocabularyTerm>
    Optional<T> equivalentValues(S from, Class<T> targetVocab, DispositionAxis axis) {
        return from.axisExactMatch(targetVocab, axis).map(targetVocab::cast);
    }

    @Override
    public Optional<VocabularyMetadata> vocabularyMetadata(String uri) {
        var clazz = byUri.get(uri);
        if (clazz == null) return Optional.empty();
        // register() guarantees @VocabularyMetadata is present for anything in byUri
        return Optional.of(clazz.getAnnotation(VocabularyMetadata.class));
    }
}

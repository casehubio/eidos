package io.casehub.eidos.api;

import java.util.List;
import java.util.Optional;

public interface VocabularyRegistry {

    // --- Registration ---

    /** Registers a vocabulary enum. The class must carry {@link VocabularyMetadata}. */
    <T extends Enum<T> & VocabularyTerm> void register(Class<T> vocab);

    boolean isRegistered(String vocabUri);

    // --- String-based resolution (runtime values / unknown vocab class at compile time) ---

    Optional<? extends VocabularyTerm> resolve(String vocabUri, String value);

    /** Returns terms in enum declaration order. Empty list if URI not registered. */
    List<? extends VocabularyTerm>     allTerms(String vocabUri);

    Optional<String> equivalentValues(String fromUri, String value, String toUri);
    Optional<String> equivalentValues(String fromUri, String value, String toUri, DispositionAxis axis);

    // --- Typed resolution (compile-time-known vocab class) ---

    /**
     * Resolves {@code value} (primary or alias) to a typed constant.
     * REQUIRES the vocabulary to be registered — uses the internal byClass index.
     */
    <T extends Enum<T> & VocabularyTerm>
        Optional<T> resolve(Class<T> vocab, String value);

    /**
     * Returns the equivalent constant in {@code targetVocab} via {@link VocabularyTerm#exactMatch}.
     * Does NOT require registration — delegates directly to the source constant's method.
     */
    <S extends Enum<S> & VocabularyTerm, T extends Enum<T> & VocabularyTerm>
        Optional<T> equivalentValues(S from, Class<T> targetVocab);

    /**
     * Returns the axis-scoped equivalent constant via {@link VocabularyTerm#axisExactMatch}.
     * Does NOT require registration — delegates directly to the source constant's method.
     */
    <S extends Enum<S> & VocabularyTerm, T extends Enum<T> & VocabularyTerm>
        Optional<T> equivalentValues(S from, Class<T> targetVocab, DispositionAxis axis);
}

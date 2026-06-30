package io.casehub.eidos.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface VocabularyRegistry {

    // --- Registration ---

    /**
     * Registers a vocabulary enum. The class must carry {@link VocabularyMetadata}.
     *
     * @throws IllegalArgumentException if the vocabulary URI is blank (annotation
     *         attributes cannot be null at runtime — blank is the only invalid state),
     *         if the vocabulary has no constants, if value/alias conflicts exist within
     *         the vocabulary, or if a different vocabulary is already registered under
     *         the same URI.
     */
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

    // --- Vocabulary-level metadata ---

    /**
     * Returns the vocabulary-level metadata annotation for the given URI.
     * Empty if the URI is not registered.
     * See {@link VocabularyMetadata} for field semantics — {@code name()},
     * {@code version()}, and {@code description()} default to {@code ""},
     * meaning "not provided"; callers should treat {@code isEmpty()} as absent.
     */
    Optional<VocabularyMetadata> vocabularyMetadata(String uri);

    // --- Hierarchy / subsumption ---

    /**
     * Tests whether {@code generalValue} subsumes {@code specificValue} in the given vocabulary.
     * @param vocabUri vocabulary URI
     * @param generalValue more general term
     * @param specificValue more specific term (candidate descendant)
     * @return true if {@code specificValue} specializes {@code generalValue}, or they are equal
     */
    boolean subsumes(String vocabUri, String generalValue, String specificValue);

    /**
     * Computes the OWLS-MX match degree between a declared capability and a requested capability.
     * @param vocabUri vocabulary URI grounding both values
     * @param declaredValue value declared in agent descriptor
     * @param requestedValue value requested at probe/query time
     * @return Exact, Plugin (declared subsumes requested), Specialization (declared is subsumed by requested), or None
     */
    MatchDegree match(String vocabUri, String declaredValue, String requestedValue);

    /**
     * Returns all ancestors (more general terms) of {@code value} in the vocabulary hierarchy.
     * @param vocabUri vocabulary URI
     * @param value term to find ancestors for
     * @return list of ancestor terms ordered by depth (closest first), or empty if none
     */
    List<? extends VocabularyTerm> ancestors(String vocabUri, String value);

    /**
     * Returns all descendants (more specific terms) of {@code value} in the vocabulary hierarchy.
     * @param vocabUri vocabulary URI
     * @param value term to find descendants for
     * @return list of descendant terms ordered by depth (closest first), or empty if none
     */
    List<? extends VocabularyTerm> descendants(String vocabUri, String value);

    /**
     * Expands a value to all related terms (ancestors and descendants) grouped by vocabulary.
     * Used by {@code AgentRegistry.find()} to match vocabulary-grounded capabilities.
     * @param value term to expand (primary value, not alias)
     * @return map of vocabulary URI → expanded term set, or empty map if value is not registered in any vocabulary
     */
    Map<String, Set<String>> expandForMatchingByVocabulary(String value);
}

package io.casehub.eidos.api;

import java.util.List;
import java.util.Optional;

/**
 * A term within a vocabulary. Implemented by enum constants.
 *
 * <p>{@link #exactMatch} and {@link #axisExactMatch} are independent. A term may implement
 * either, both, or neither. The registry routes axis-aware and axis-unaware lookups to the
 * appropriate method independently — calling the axis-unaware overload against a DISC term
 * (which only implements axisExactMatch) returns {@code Optional.empty()}, which is correct.
 */
public interface VocabularyTerm {
    String value();
    String label();
    /** Returns {@code ""} when no description was provided. Callers treat {@code isEmpty()} as absent. */
    default String description()   { return ""; }
    default List<String> aliases() { return List.of(); }

    /**
     * Axis-unaware cross-vocabulary equivalence.
     * Returns the equivalent constant in {@code targetVocab}, or empty if none.
     * The registry's typed overload calls {@code targetVocab.cast()} on the result.
     */
    default Optional<VocabularyTerm> exactMatch(Class<?> targetVocab) {
        return Optional.empty();
    }

    /**
     * Axis-aware cross-vocabulary equivalence.
     *
     * <p>Implementations covering a given {@code targetVocab} MUST use an exhaustive switch
     * on {@code axis} with no default branch — adding a new {@link DispositionAxis} value
     * then causes a compile error, forcing explicit coverage of the new axis.
     * {@code Optional.empty()} is a valid branch for axes with no meaningful mapping.
     * Do NOT wrap the switch in {@code Optional.of()} — that forbids gaps.
     *
     * <p>The exhaustive switch enforces completeness for the axis dimension only. Adding a
     * new target vocabulary requires a new {@code if (targetVocab == ...)} branch;
     * no compile-time enforcement exists for the target-vocabulary dimension.
     */
    default Optional<VocabularyTerm> axisExactMatch(Class<?> targetVocab, DispositionAxis axis) {
        return Optional.empty();
    }
}

package io.casehub.eidos.api;

/**
 * OWLS-MX semantic match degree between a declared capability and a requested capability.
 * Based on the OWL-S Matchmaker (OWLS-MX) matching framework.
 *
 * <p>Match degrees in descending priority:
 * <ul>
 *   <li>{@link Exact} — declared and requested are identical
 *   <li>{@link Plugin} — declared subsumes requested (declared is more general)
 *   <li>{@link Specialization} — requested subsumes declared (declared is more specific)
 *   <li>{@link None} — no semantic relationship
 * </ul>
 */
public sealed interface MatchDegree
        permits MatchDegree.Exact, MatchDegree.Plugin,
                MatchDegree.Specialization, MatchDegree.None {

    /** Exact match — declared and requested values are identical. */
    record Exact() implements MatchDegree {}

    /**
     * Plugin match — declared capability is more general (parent) than requested.
     * @param depth distance in the hierarchy (1 = direct parent, 2 = grandparent, etc.)
     */
    record Plugin(int depth) implements MatchDegree {}

    /**
     * Specialization match — declared capability is more specific (child) than requested.
     * @param depth distance in the hierarchy (1 = direct child, 2 = grandchild, etc.)
     */
    record Specialization(int depth) implements MatchDegree {}

    /** No match — declared and requested have no semantic relationship. */
    record None() implements MatchDegree {}
}

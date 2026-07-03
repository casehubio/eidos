package io.casehub.eidos.api;

import java.util.List;

/**
 * Shared subsumption resolution utility for capabilities.
 * Used by both probe paths and recording paths to match declared capabilities
 * against requested capability tags using vocabulary-grounded subsumption.
 *
 * <p>This utility eliminates duplication and ensures consistent matching logic
 * across {@link CapabilityHealth#probe(AgentDescriptor, String, ProbeContext)}
 * and {@link BehavioralSignalStore} learned exclusion lookups.
 *
 * @since 0.2
 */
public final class CapabilityResolver {

    private CapabilityResolver() {}

    /**
     * Computes the match degree between a declared capability and a requested capability tag.
     *
     * <p>Matching logic:
     * <ul>
     *   <li>Exact name match → {@link MatchDegree.Exact}
     *   <li>Ungrounded capability (no {@code capabilityVocabulary}) → {@link MatchDegree.None}
     *   <li>Grounded capability → delegate to {@link VocabularyRegistry#match(String, String, String)}
     * </ul>
     *
     * @param capability the declared capability from an {@link AgentDescriptor}
     * @param capabilityTag the requested capability tag (e.g., from probe or query)
     * @param registry the vocabulary registry for subsumption resolution
     * @return the match degree (Exact, Plugin, Specialization, or None)
     */
    public static MatchDegree match(final AgentCapability capability,
                                     final String capabilityTag,
                                     final VocabularyRegistry registry) {
        if (capability.name().equals(capabilityTag)) {
            return new MatchDegree.Exact();
        }

        if (capability.capabilityVocabulary() == null
                || capability.capabilityVocabulary().isBlank()) {
            return new MatchDegree.None();
        }

        return registry.match(
            capability.capabilityVocabulary(),
            capability.name(),
            capabilityTag
        );
    }

    /**
     * Resolves the best matching capability from a list of declared capabilities.
     *
     * <p>Selection uses {@link MatchDegree#compareTo} — Exact wins immediately,
     * then the lowest-ranked (best) non-None degree. First in list wins at equal rank.
     *
     * @param capabilities the list of declared capabilities to search
     * @param capabilityTag the requested capability tag
     * @param registry the vocabulary registry for subsumption resolution
     * @return the best matching capability with its degree, or {@code null} if no match found
     */
    public static ResolvedCapability resolve(final List<AgentCapability> capabilities,
                                              final String capabilityTag,
                                              final VocabularyRegistry registry) {
        if (capabilities == null || capabilities.isEmpty()) {
            return null;
        }

        ResolvedCapability best = null;

        for (final var capability : capabilities) {
            final MatchDegree degree = match(capability, capabilityTag, registry);

            if (degree instanceof MatchDegree.Exact) {
                return new ResolvedCapability(capability, degree);
            }
            if (!(degree instanceof MatchDegree.None)) {
                if (best == null || degree.compareTo(best.degree()) < 0) {
                    best = new ResolvedCapability(capability, degree);
                }
            }
        }

        return best;
    }
}

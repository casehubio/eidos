package io.casehub.eidos.api;

import java.util.List;

/**
 * Shared subsumption resolution utility for capabilities.
 * Used by both probe paths and recording paths to match declared capabilities
 * against requested capability tags using vocabulary-grounded subsumption.
 *
 * <p>This utility eliminates duplication and ensures consistent matching logic
 * across {@link CapabilityHealth#probe(AgentDescriptor, String, ProbeContext)}
 * and {@link CapabilitySpecializationStore} learned exclusion lookups.
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
     * <p>Selection priority:
     * <ol>
     *   <li>Exact match (returns immediately)
     *   <li>Closest subsumption match (Plugin or Specialization with smallest depth)
     *   <li>First in list wins at equal depth
     * </ol>
     *
     * @param capabilities the list of declared capabilities to search
     * @param capabilityTag the requested capability tag
     * @param registry the vocabulary registry for subsumption resolution
     * @return the best matching capability, or {@code null} if no match found
     */
    public static AgentCapability resolve(final List<AgentCapability> capabilities,
                                           final String capabilityTag,
                                           final VocabularyRegistry registry) {
        if (capabilities == null || capabilities.isEmpty()) {
            return null;
        }

        AgentCapability bestMatch = null;
        int bestDepth = Integer.MAX_VALUE;

        for (final var capability : capabilities) {
            final MatchDegree degree = match(capability, capabilityTag, registry);

            if (degree instanceof MatchDegree.Exact) {
                return capability;
            } else if (degree instanceof MatchDegree.Plugin plugin) {
                if (plugin.depth() < bestDepth) {
                    bestMatch = capability;
                    bestDepth = plugin.depth();
                }
            } else if (degree instanceof MatchDegree.Specialization specialization) {
                if (specialization.depth() < bestDepth) {
                    bestMatch = capability;
                    bestDepth = specialization.depth();
                }
            }
        }

        return bestMatch;
    }
}

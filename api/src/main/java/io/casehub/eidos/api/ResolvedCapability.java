package io.casehub.eidos.api;

/**
 * Result of resolving a capability tag against declared capabilities via
 * {@link CapabilityResolver#resolve(java.util.List, String, VocabularyRegistry)}.
 *
 * @param capability the declared capability that matched
 * @param degree the OWLS-MX match degree
 */
public record ResolvedCapability(AgentCapability capability, MatchDegree degree) {}

package io.casehub.eidos.api;

@FunctionalInterface
public interface LivedDispositionSource {

    /**
     * Returns the effective disposition for the given agent, composing any
     * evolved personality state on top of the authored baseline.
     *
     * <p>When no evolution exists, returns {@code authored} as-is.
     * Implementations receive the authored disposition so they can perform
     * fingerprint checks against the authored baseline without calling back
     * to the registry.
     */
    AgentDisposition resolveEffective(String agentId, String tenancyId, AgentDisposition authored);
}

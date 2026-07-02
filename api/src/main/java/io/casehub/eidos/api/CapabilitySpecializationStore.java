package io.casehub.eidos.api;

import java.util.Map;

public interface CapabilitySpecializationStore {

    /**
     * Records one signal event for the given agent, capability, and domain.
     * TTL is owned by the store implementation — per-signal TTL is supported.
     *
     * <p>{@code capabilityName} must be the agent's declared capability name
     * (as returned by {@link AgentCapability#name()}), not a query/lookup term.
     * When the caller has a query tag instead, use
     * {@link CapabilityResolver#resolve(java.util.List, String, VocabularyRegistry)}
     * to obtain the declared capability first.
     */
    void record(String agentId, String tenancyId, String capabilityName,
                String domain, SpecializationSignal signal);

    /**
     * Retracts all learned data of the given signal type for an
     * (agentId, tenancyId, capabilityName) triple.
     * Clears all domain entries regardless of TTL.
     *
     * <p>{@code capabilityName} must be the agent's declared capability name —
     * see {@link #record} for details.
     */
    void clear(String agentId, String tenancyId, String capabilityName,
               SpecializationSignal signal);

    /**
     * Returns domain to count of unexpired records for the given signal type,
     * for all domains with at least one unexpired record.
     * Empty map when none. Never null.
     *
     * <p>{@code capabilityName} must be the agent's declared capability name —
     * see {@link #record} for details.
     */
    Map<String, Integer> learned(String agentId, String tenancyId,
                                 String capabilityName, SpecializationSignal signal);

    /**
     * Returns the count of unexpired records for the given signal type and domain.
     * 0 when no unexpired records exist. Never negative.
     *
     * <p>{@code capabilityName} must be the agent's declared capability name —
     * see {@link #record} for details.
     */
    int count(String agentId, String tenancyId, String capabilityName,
              String domain, SpecializationSignal signal);
}

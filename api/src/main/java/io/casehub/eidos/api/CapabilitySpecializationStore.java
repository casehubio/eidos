package io.casehub.eidos.api;

import java.util.Map;

public interface CapabilitySpecializationStore {

    /**
     * Records one DECLINE for the given agent, capability, and domain.
     * Called by casehub-ledger/CBR per qualifying DECLINE attestation.
     * TTL is owned by the store implementation.
     */
    void recordDecline(String agentId, String tenancyId, String capabilityName, String domain);

    /**
     * Retracts all learned data for a (agentId, tenancyId, capabilityName) triple.
     * Clears all domain entries regardless of TTL. Emergency override.
     */
    void clearDeclines(String agentId, String tenancyId, String capabilityName);

    /**
     * Returns domain → count of unexpired DECLINE records for all domains with at least
     * 1 unexpired recorded decline. Empty map when none. Never null.
     */
    Map<String, Integer> learnedExclusions(String agentId, String tenancyId, String capabilityName);

    /**
     * Returns the count of unexpired DECLINE records for the given domain.
     * 0 when no unexpired records exist. Never negative.
     */
    int declineCount(String agentId, String tenancyId, String capabilityName, String domain);
}

package io.casehub.eidos.api;

import java.util.List;
import java.util.Optional;

public interface AgentRegistry {
    void register(AgentDescriptor descriptor);

    /**
     * @throws NullPointerException if agentId or tenancyId is null
     */
    Optional<AgentDescriptor> findById(String agentId, String tenancyId);

    /**
     * Finds agents matching the query criteria.
     *
     * <p>When {@link AgentQuery#capabilityName()} is non-null, results carry
     * {@link AgentMatch#resolvedCapability()} and are ordered by match quality
     * (best first per OWLS-MX ordering). When no capability is queried,
     * {@code resolvedCapability} is null and ordering is unspecified.
     */
    List<AgentMatch> find(AgentQuery query);
}

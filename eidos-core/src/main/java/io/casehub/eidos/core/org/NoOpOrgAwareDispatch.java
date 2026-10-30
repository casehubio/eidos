package io.casehub.eidos.core.org;

import io.casehub.eidos.api.AgentMatch;
import io.casehub.eidos.api.OrgAwareDispatch;

import java.util.List;
import java.util.Optional;

public class NoOpOrgAwareDispatch implements OrgAwareDispatch {

    @Override
    public Optional<AgentMatch> escalate(String agentId, String capabilityName,
                                         String tenancyId) {
        return Optional.empty();
    }

    @Override
    public boolean canDelegateTo(String fromAgentId, String toAgentId,
                                 String tenancyId) {
        return true;
    }

    @Override
    public List<String> teamMembers(String agentId, String tenancyId) {
        return List.of();
    }
}

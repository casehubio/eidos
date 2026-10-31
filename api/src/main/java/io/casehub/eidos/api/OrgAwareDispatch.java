package io.casehub.eidos.api;

import java.util.List;
import java.util.Optional;

public interface OrgAwareDispatch {

    Optional<AgentMatch> escalate(String agentId, String capabilityName, String tenancyId);

    boolean canDelegateTo(String fromAgentId, String toAgentId, String tenancyId);

    List<String> teamMembers(String agentId, String tenancyId);

    int orgDistance(String agentIdA, String agentIdB, String tenancyId);

}

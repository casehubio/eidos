package io.casehub.eidos.org.api;

import java.util.List;
import java.util.Optional;

public interface GroupLifecycleService {

    GroupInstance createGroup(String specId, String tenancyId, FormationOrigin origin);
    GroupInstance proposeGroup(String specId, String tenancyId, String proposerAgentId);
    void approveProposal(String instanceId, String tenancyId);
    void rejectProposal(String instanceId, String tenancyId, String reason);
    void activateGroup(String instanceId, String tenancyId);
    void dissolveGroup(String instanceId, String tenancyId, String reason);

    void changeTopology(String instanceId, String newTopology, String newTopologyVocabulary, String tenancyId);

    void joinGroup(String instanceId, String agentId, String tenancyId);
    void leaveGroup(String instanceId, String agentId, String tenancyId);

    void adoptRole(String instanceId, String agentId, String roleName, String tenancyId);
    void releaseRole(String instanceId, String agentId, String roleName, String tenancyId);

    void commitToMission(String instanceId, String agentId, String missionName, String tenancyId);
    void withdrawFromMission(String instanceId, String agentId, String missionName, String tenancyId);
    void fulfillMission(String instanceId, String missionName, String tenancyId);

    Optional<GroupInstance> findInstance(String instanceId, String tenancyId);
    List<GroupInstance> findInstances(GroupInstanceQuery query);
    List<GroupInstance> instancesForAgent(String agentId, String tenancyId);
}

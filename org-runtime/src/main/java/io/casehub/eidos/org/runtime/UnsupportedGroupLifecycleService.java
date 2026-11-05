package io.casehub.eidos.org.runtime;

import io.casehub.eidos.org.api.FormationOrigin;
import io.casehub.eidos.org.api.GroupInstance;
import io.casehub.eidos.org.api.GroupInstanceQuery;
import io.casehub.eidos.org.api.GroupLifecycleService;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@DefaultBean
@ApplicationScoped
public class UnsupportedGroupLifecycleService implements GroupLifecycleService {

    @Override
    public GroupInstance createGroup(String specId, String tenancyId, FormationOrigin origin) {
        throw new UnsupportedOperationException("No GroupLifecycleService configured");
    }

    @Override
    public GroupInstance proposeGroup(String specId, String tenancyId, String proposerAgentId) {
        throw new UnsupportedOperationException("No GroupLifecycleService configured");
    }

    @Override
    public void approveProposal(String instanceId, String tenancyId) {
        throw new UnsupportedOperationException("No GroupLifecycleService configured");
    }

    @Override
    public void rejectProposal(String instanceId, String tenancyId, String reason) {
        throw new UnsupportedOperationException("No GroupLifecycleService configured");
    }

    @Override
    public void activateGroup(String instanceId, String tenancyId) {
        throw new UnsupportedOperationException("No GroupLifecycleService configured");
    }

    @Override
    public void dissolveGroup(String instanceId, String tenancyId, String reason) {
        throw new UnsupportedOperationException("No GroupLifecycleService configured");
    }

    @Override
    public void changeTopology(String instanceId, String newTopology, String newTopologyVocabulary, String tenancyId) {
        throw new UnsupportedOperationException("No GroupLifecycleService configured");
    }

    @Override
    public void joinGroup(String instanceId, String agentId, String tenancyId) {
        throw new UnsupportedOperationException("No GroupLifecycleService configured");
    }

    @Override
    public void leaveGroup(String instanceId, String agentId, String tenancyId) {
        throw new UnsupportedOperationException("No GroupLifecycleService configured");
    }

    @Override
    public void adoptRole(String instanceId, String agentId, String roleName, String tenancyId) {
        throw new UnsupportedOperationException("No GroupLifecycleService configured");
    }

    @Override
    public void releaseRole(String instanceId, String agentId, String roleName, String tenancyId) {
        throw new UnsupportedOperationException("No GroupLifecycleService configured");
    }

    @Override
    public void commitToMission(String instanceId, String agentId, String missionName, String tenancyId) {
        throw new UnsupportedOperationException("No GroupLifecycleService configured");
    }

    @Override
    public void withdrawFromMission(String instanceId, String agentId, String missionName, String tenancyId) {
        throw new UnsupportedOperationException("No GroupLifecycleService configured");
    }

    @Override
    public void fulfillMission(String instanceId, String missionName, String tenancyId) {
        throw new UnsupportedOperationException("No GroupLifecycleService configured");
    }

    @Override
    public Optional<GroupInstance> findInstance(String instanceId, String tenancyId) {
        throw new UnsupportedOperationException("No GroupLifecycleService configured");
    }

    @Override
    public List<GroupInstance> findInstances(GroupInstanceQuery query) {
        throw new UnsupportedOperationException("No GroupLifecycleService configured");
    }

    @Override
    public List<GroupInstance> instancesForAgent(String agentId, String tenancyId) {
        throw new UnsupportedOperationException("No GroupLifecycleService configured");
    }
}

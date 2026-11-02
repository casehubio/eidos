package io.casehub.eidos.org.api;

public interface GroupLifecycleListener {
    default void onFormationProposal(FormationProposal proposal) {}
    default void onFormationComplete(String instanceId, String tenancyId) {}
    default void onDissolution(String instanceId, String tenancyId, String reason) {}
    default void onMemberJoined(String instanceId, String agentId, String tenancyId) {}
    default void onMemberLeft(String instanceId, String agentId, String tenancyId) {}
    default void onRoleAdoption(String instanceId, String agentId, String roleName, String tenancyId) {}
    default void onRoleRelease(String instanceId, String agentId, String roleName, String tenancyId) {}
    default void onTopologyChange(String instanceId, String oldTopology, String newTopology, String tenancyId) {}
    default void onStateTransition(String instanceId, GroupLifecycleState fromState, GroupLifecycleState toState, String tenancyId) {}
}

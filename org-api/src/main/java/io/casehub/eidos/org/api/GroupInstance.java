package io.casehub.eidos.org.api;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record GroupInstance(
    String instanceId,
    String specId,
    String tenancyId,
    GroupLifecycleState state,
    FormationOrigin formationOrigin,
    String topology,
    String topologyVocabulary,
    AuthorityModel effectiveAuthority,
    List<ActualMembership> members,
    List<RoleAdoption> roleAdoptions,
    List<MissionCommitment> missionCommitments,
    Instant createdAt,
    Instant lastTransitionAt,
    long version
) {
    public GroupInstance {
        Objects.requireNonNull(instanceId, "instanceId");
        Objects.requireNonNull(specId, "specId");
        Objects.requireNonNull(tenancyId, "tenancyId");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(formationOrigin, "formationOrigin");
        members = members != null ? List.copyOf(members) : List.of();
        roleAdoptions = roleAdoptions != null ? List.copyOf(roleAdoptions) : List.of();
        missionCommitments = missionCommitments != null ? List.copyOf(missionCommitments) : List.of();
        if (version < 0) throw new OrgValidationException("version", "must be >= 0 (was " + version + ")");
        Set<String> memberIds = members.stream().map(ActualMembership::agentId).collect(Collectors.toSet());
        for (var ra : roleAdoptions) {
            if (!memberIds.contains(ra.agentId()))
                throw new OrgValidationException("roleAdoptions",
                    "agent " + ra.agentId() + " has role adoption but is not a member (membership invariant)");
        }
    }

    public GroupInstance withState(GroupLifecycleState newState) {
        return new GroupInstance(instanceId, specId, tenancyId, newState, formationOrigin,
                                 topology, topologyVocabulary, effectiveAuthority, members, roleAdoptions,
                                 missionCommitments, createdAt, lastTransitionAt, version);
    }

    public GroupInstance withTopology(String newTopology, String newTopologyVocabulary) {
        return new GroupInstance(instanceId, specId, tenancyId, state, formationOrigin,
                                 newTopology, newTopologyVocabulary, effectiveAuthority, members, roleAdoptions,
                                 missionCommitments, createdAt, lastTransitionAt, version);
    }

    public GroupInstance withEffectiveAuthority(AuthorityModel newAuthority) {
        return new GroupInstance(instanceId, specId, tenancyId, state, formationOrigin,
                                 topology, topologyVocabulary, newAuthority, members, roleAdoptions,
                                 missionCommitments, createdAt, lastTransitionAt, version);
    }

    public GroupInstance withMembers(List<ActualMembership> newMembers) {
        return new GroupInstance(instanceId, specId, tenancyId, state, formationOrigin,
                                 topology, topologyVocabulary, effectiveAuthority, newMembers, roleAdoptions,
                                 missionCommitments, createdAt, lastTransitionAt, version);
    }

    public GroupInstance withRoleAdoptions(List<RoleAdoption> newAdoptions) {
        return new GroupInstance(instanceId, specId, tenancyId, state, formationOrigin,
                                 topology, topologyVocabulary, effectiveAuthority, members, newAdoptions,
                                 missionCommitments, createdAt, lastTransitionAt, version);
    }

    public GroupInstance withMissionCommitments(List<MissionCommitment> newCommitments) {
        return new GroupInstance(instanceId, specId, tenancyId, state, formationOrigin,
                                 topology, topologyVocabulary, effectiveAuthority, members, roleAdoptions,
                                 newCommitments, createdAt, lastTransitionAt, version);
    }

    public GroupInstance withLastTransitionAt(Instant newLastTransitionAt) {
        return new GroupInstance(instanceId, specId, tenancyId, state, formationOrigin,
                                 topology, topologyVocabulary, effectiveAuthority, members, roleAdoptions,
                                 missionCommitments, createdAt, newLastTransitionAt, version);
    }

    public GroupInstance withVersion(long newVersion) {
        return new GroupInstance(instanceId, specId, tenancyId, state, formationOrigin,
                                 topology, topologyVocabulary, effectiveAuthority, members, roleAdoptions,
                                 missionCommitments, createdAt, lastTransitionAt, newVersion);
    }
}

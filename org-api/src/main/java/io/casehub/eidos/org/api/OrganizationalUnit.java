package io.casehub.eidos.org.api;

import io.casehub.eidos.api.AgentCapability;
import io.casehub.eidos.api.AgentConstraint;
import io.casehub.eidos.api.AgentGoal;

import java.util.List;
import java.util.Objects;

public record OrganizationalUnit(
        String unitId,
        String name,
        String kind,
        String kindVocabulary,
        String tenancyId,
        String parentUnitId,
        String topology,
        String topologyVocabulary,
        AuthorityModel authorityModel,
        double selfDetermination,
        GroupPolicy policy,
        List<Membership> members,
        List<RoleSpec> roleSpecs,
        List<MissionSpec> missions,
        List<AgentCapability> capabilities,
        List<AgentGoal> goals,
        List<AgentConstraint> constraints
) {
    public static final int MAX_MEMBERS = 50;

    public OrganizationalUnit {
        Objects.requireNonNull(unitId, "unitId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(tenancyId, "tenancyId");
        if (selfDetermination < 0.0 || selfDetermination > 1.0) {
            throw new OrgValidationException("selfDetermination", "must be in [0.0, 1.0] (was " + selfDetermination + ")");
        }
        members      = members != null ? List.copyOf(members) : List.of();
        roleSpecs    = roleSpecs != null ? List.copyOf(roleSpecs) : List.of();
        missions     = missions != null ? List.copyOf(missions) : List.of();
        capabilities = capabilities != null ? List.copyOf(capabilities) : List.of();
        goals        = goals != null ? List.copyOf(goals) : List.of();
        constraints  = constraints != null ? List.copyOf(constraints) : List.of();
        if (members.size() > MAX_MEMBERS) {
            throw new OrgValidationException("members",
                                             "exceeds maximum count " + MAX_MEMBERS + " (was " + members.size() + ")");
        }
        // Multi-role seeding: reject duplicate (agentId, role) pairs, not duplicate agentId
        if (members.size() > 1) {
            long distinctPairs = members.stream()
                                        .map(m -> m.agentId() + ":" + (m.role() != null ? m.role() : ""))
                                        .distinct().count();
            if (distinctPairs < members.size()) {
                throw new OrgValidationException("members", "duplicate (agentId, role) pair in membership list");
            }
        }
        // Validate roleSpec uniqueness
        if (roleSpecs.size() > 1) {
            long distinctRoles = roleSpecs.stream().map(RoleSpec::roleName).distinct().count();
            if (distinctRoles < roleSpecs.size()) {
                throw new OrgValidationException("roleSpecs", "duplicate roleName in roleSpec list");
            }
        }
        // Validate incompatibleWith references
        var roleNames = roleSpecs.stream().map(RoleSpec::roleName).collect(java.util.stream.Collectors.toSet());
        for (var rs : roleSpecs) {
            for (var incompat : rs.incompatibleWith()) {
                if (!roleNames.contains(incompat)) {
                    throw new OrgValidationException("incompatibleWith",
                                                     "role '" + rs.roleName() + "' declares incompatibility with unknown role '" + incompat + "'");
                }
            }
        }
        // Validate seed membership incompatibility
        if (members.size() > 1 && !roleSpecs.isEmpty()) {
            var roleSpecMap = roleSpecs.stream().collect(java.util.stream.Collectors.toMap(RoleSpec::roleName, r -> r));
            var agentRoles  = new java.util.HashMap<String, java.util.List<String>>();
            for (var m : members) {
                if (m.role() != null) {
                    agentRoles.computeIfAbsent(m.agentId(), k -> new java.util.ArrayList<>()).add(m.role());
                }
            }
            for (var entry : agentRoles.entrySet()) {
                var roles = entry.getValue();
                for (int i = 0; i < roles.size(); i++) {
                    for (int j = i + 1; j < roles.size(); j++) {
                        var specI = roleSpecMap.get(roles.get(i));
                        var specJ = roleSpecMap.get(roles.get(j));
                        if (specI != null && specI.incompatibleWith().contains(roles.get(j))) {
                            throw new OrgValidationException("members",
                                                             "agent '" + entry.getKey() + "' has incompatible seed roles: '" + roles.get(i) + "' and '" + roles.get(j) + "'");
                        }
                        if (specJ != null && specJ.incompatibleWith().contains(roles.get(i))) {
                            throw new OrgValidationException("members",
                                                             "agent '" + entry.getKey() + "' has incompatible seed roles: '" + roles.get(j) + "' and '" + roles.get(i) + "'");
                        }
                    }
                }
            }
        }
        // Validate mission uniqueness
        if (missions.size() > 1) {
            long distinctMissions = missions.stream().map(MissionSpec::missionName).distinct().count();
            if (distinctMissions < missions.size()) {
                throw new OrgValidationException("missions", "duplicate missionName in mission list");
            }
        }
    }

    public boolean hasMember(String agentId) {
        return members.stream().anyMatch(m -> m.agentId().equals(agentId));
    }

    public static Builder builder() {return new Builder();}

    public static final class Builder {
        private String unitId, name, kind, kindVocabulary, tenancyId, parentUnitId;
        private String topology, topologyVocabulary;
        private AuthorityModel        authorityModel;
        private double                selfDetermination;
        private GroupPolicy           policy;
        private List<Membership>      members;
        private List<RoleSpec>        roleSpecs;
        private List<MissionSpec>     missions;
        private List<AgentCapability> capabilities;
        private List<AgentGoal>       goals;
        private List<AgentConstraint> constraints;

        public Builder unitId(String v)                      {
                                                                 this.unitId = v;
                                                                 return this;
                                                             }

        public Builder name(String v)                        {
                                                                 this.name = v;
                                                                 return this;
                                                             }

        public Builder kind(String v)                        {
                                                                 this.kind = v;
                                                                 return this;
                                                             }

        public Builder kindVocabulary(String v)              {
                                                                 this.kindVocabulary = v;
                                                                 return this;
                                                             }

        public Builder tenancyId(String v)                   {
                                                                 this.tenancyId = v;
                                                                 return this;
                                                             }

        public Builder parentUnitId(String v)                {
                                                                 this.parentUnitId = v;
                                                                 return this;
                                                             }

        public Builder topology(String v)                    {
                                                                 this.topology = v;
                                                                 return this;
                                                             }

        public Builder topologyVocabulary(String v)          {
                                                                 this.topologyVocabulary = v;
                                                                 return this;
                                                             }

        public Builder authorityModel(AuthorityModel v)      {
                                                                 this.authorityModel = v;
                                                                 return this;
                                                             }

        public Builder selfDetermination(double v)           {
                                                                 this.selfDetermination = v;
                                                                 return this;
                                                             }

        public Builder policy(GroupPolicy v)                 {
                                                                 this.policy = v;
                                                                 return this;
                                                             }

        public Builder members(List<Membership> v)           {
                                                                 this.members = v;
                                                                 return this;
                                                             }

        public Builder roleSpecs(List<RoleSpec> v)           {
                                                                 this.roleSpecs = v;
                                                                 return this;
                                                             }

        public Builder missions(List<MissionSpec> v)         {
                                                                 this.missions = v;
                                                                 return this;
                                                             }

        public Builder capabilities(List<AgentCapability> v) {
                                                                 this.capabilities = v;
                                                                 return this;
                                                             }

        public Builder goals(List<AgentGoal> v)              {
                                                                 this.goals = v;
                                                                 return this;
                                                             }

        public Builder constraints(List<AgentConstraint> v)  {
                                                                 this.constraints = v;
                                                                 return this;
                                                             }

        public OrganizationalUnit build() {
            return new OrganizationalUnit(unitId, name, kind, kindVocabulary, tenancyId,
                                          parentUnitId, topology, topologyVocabulary, authorityModel, selfDetermination,
                                          policy, members, roleSpecs, missions, capabilities, goals, constraints);
        }
    }
}

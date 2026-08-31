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
    List<Membership> members,
    List<AgentCapability> capabilities,
    List<AgentGoal> goals,
    List<AgentConstraint> constraints
) {
    public static final int MAX_MEMBERS = 50;

    public OrganizationalUnit {
        Objects.requireNonNull(unitId, "unitId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(tenancyId, "tenancyId");
        members = members != null ? List.copyOf(members) : List.of();
        capabilities = capabilities != null ? List.copyOf(capabilities) : List.of();
        goals = goals != null ? List.copyOf(goals) : List.of();
        constraints = constraints != null ? List.copyOf(constraints) : List.of();
        if (members.size() > MAX_MEMBERS) {
            throw new OrgValidationException("members",
                "exceeds maximum count " + MAX_MEMBERS + " (was " + members.size() + ")");
        }
        if (members.size() > 1) {
            long distinctAgents = members.stream().map(Membership::agentId).distinct().count();
            if (distinctAgents < members.size()) {
                throw new OrgValidationException("members", "duplicate agentId in membership list");
            }
        }
    }

    public boolean hasMember(String agentId) {
        return members.stream().anyMatch(m -> m.agentId().equals(agentId));
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String unitId, name, kind, kindVocabulary, tenancyId, parentUnitId;
        private List<Membership> members;
        private List<AgentCapability> capabilities;
        private List<AgentGoal> goals;
        private List<AgentConstraint> constraints;

        public Builder unitId(String v) { this.unitId = v; return this; }
        public Builder name(String v) { this.name = v; return this; }
        public Builder kind(String v) { this.kind = v; return this; }
        public Builder kindVocabulary(String v) { this.kindVocabulary = v; return this; }
        public Builder tenancyId(String v) { this.tenancyId = v; return this; }
        public Builder parentUnitId(String v) { this.parentUnitId = v; return this; }
        public Builder members(List<Membership> v) { this.members = v; return this; }
        public Builder capabilities(List<AgentCapability> v) { this.capabilities = v; return this; }
        public Builder goals(List<AgentGoal> v) { this.goals = v; return this; }
        public Builder constraints(List<AgentConstraint> v) { this.constraints = v; return this; }

        public OrganizationalUnit build() {
            return new OrganizationalUnit(unitId, name, kind, kindVocabulary, tenancyId,
                parentUnitId, members, capabilities, goals, constraints);
        }
    }
}

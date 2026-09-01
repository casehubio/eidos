package io.casehub.eidos.org.api;

import io.casehub.eidos.api.AgentCapability;
import io.casehub.eidos.api.AgentConstraint;
import io.casehub.eidos.api.AgentGoal;
import io.casehub.eidos.api.BehavioralSignal;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class OrgStructure {

    private final String tenancyId;
    private final List<OrganizationalUnit> units = new ArrayList<>();
    private final List<AgentRelationship> relationships = new ArrayList<>();

    private OrgStructure(String tenancyId) {
        this.tenancyId = tenancyId;
    }

    public static OrgStructure define(String tenancyId) {
        return new OrgStructure(tenancyId);
    }

    public UnitBuilder unit(String unitId) {
        return new UnitBuilder(this, unitId);
    }

    public RelBuilder supervises(String source, String target) {
        return new RelBuilder(this, source, target, RelationshipKind.SUPERVISES);
    }

    public RelBuilder delegatesTo(String source, String target) {
        return new RelBuilder(this, source, target, RelationshipKind.DELEGATES_TO);
    }

    public RelBuilder escalatesTo(String source, String target) {
        return new RelBuilder(this, source, target, RelationshipKind.ESCALATES_TO);
    }

    public RelBuilder reportsTo(String source, String target) {
        return new RelBuilder(this, source, target, RelationshipKind.REPORTS_TO);
    }

    public RelBuilder backsUp(String source, String target) {
        return new RelBuilder(this, source, target, RelationshipKind.BACKS_UP);
    }

    public RelBuilder extended(String source, String target, String extendedKind) {
        return new RelBuilder(this, source, target, RelationshipKind.EXTENDED)
            .extendedKind(extendedKind);
    }

    public Result build() {
        return new Result(List.copyOf(units), List.copyOf(relationships));
    }

    public record Result(
        List<OrganizationalUnit> units,
        List<AgentRelationship> relationships
    ) {
        public void registerAll(OrgRegistry registry) {
            units.forEach(registry::registerUnit);
            relationships.forEach(registry::addRelationship);
        }
    }

    public static final class UnitBuilder {
        private final OrgStructure parent;
        private final String unitId;
        private String name;
        private String kind;
        private String kindVocabulary;
        private String parentUnitId;
        private final List<Membership> members = new ArrayList<>();
        private final List<AgentCapability> capabilities = new ArrayList<>();
        private final List<AgentGoal> goals = new ArrayList<>();
        private final List<AgentConstraint> constraints = new ArrayList<>();

        UnitBuilder(OrgStructure parent, String unitId) {
            this.parent = parent;
            this.unitId = unitId;
            this.name = unitId;
        }

        public UnitBuilder name(String v) { this.name = v; return this; }
        public UnitBuilder kind(String v) { this.kind = v; return this; }
        public UnitBuilder kindVocabulary(String v) { this.kindVocabulary = v; return this; }
        public UnitBuilder parentUnit(String v) { this.parentUnitId = v; return this; }

        public UnitBuilder member(String agentId) {
            members.add(new Membership(agentId, null, null));
            return this;
        }

        public UnitBuilder member(String agentId, String role) {
            members.add(new Membership(agentId, role, null));
            return this;
        }

        public UnitBuilder member(String agentId, String role, String roleVocabulary) {
            members.add(new Membership(agentId, role, roleVocabulary));
            return this;
        }

        public UnitBuilder capability(AgentCapability cap) {
            capabilities.add(cap);
            return this;
        }

        public UnitBuilder capability(String name) {
            capabilities.add(AgentCapability.builder().name(name).build());
            return this;
        }

        public UnitBuilder goal(AgentGoal goal) {
            goals.add(goal);
            return this;
        }

        public UnitBuilder constraint(AgentConstraint constraint) {
            constraints.add(constraint);
            return this;
        }

        public OrgStructure add() {
            parent.units.add(new OrganizationalUnit(
                unitId, name, kind, kindVocabulary, parent.tenancyId,
                parentUnitId, members, capabilities, goals, constraints));
            return parent;
        }
    }

    public static final class RelBuilder {
        private final OrgStructure parent;
        private final String source;
        private final String target;
        private final RelationshipKind kind;
        private String extendedKind;
        private String kindVocabulary;
        private RelationshipScope scope;
        private AttestationGrant attestation;

        RelBuilder(OrgStructure parent, String source, String target, RelationshipKind kind) {
            this.parent = parent;
            this.source = source;
            this.target = target;
            this.kind = kind;
        }

        RelBuilder extendedKind(String v) { this.extendedKind = v; return this; }
        public RelBuilder kindVocabulary(String v) { this.kindVocabulary = v; return this; }

        public RelBuilder scope(String capabilityName) {
            this.scope = new RelationshipScope(capabilityName, null, null);
            return this;
        }

        public RelBuilder scope(String capabilityName, String domain) {
            this.scope = new RelationshipScope(capabilityName, domain, null);
            return this;
        }

        public RelBuilder scope(RelationshipScope s) {
            this.scope = s;
            return this;
        }

        public RelBuilder attestation(Set<String> dimensions) {
            this.attestation = new AttestationGrant(dimensions, Set.of(), Set.of());
            return this;
        }

        public RelBuilder attestation(Set<String> dimensions, Set<BehavioralSignal> signalTypes) {
            this.attestation = new AttestationGrant(dimensions, Set.of(), signalTypes);
            return this;
        }

        public RelBuilder attestation(AttestationGrant grant) {
            this.attestation = grant;
            return this;
        }

        public OrgStructure add() {
            parent.relationships.add(new AgentRelationship(
                source, target, kind, extendedKind, kindVocabulary,
                scope, attestation, parent.tenancyId));
            return parent;
        }
    }
}

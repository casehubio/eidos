package io.casehub.eidos.org.annotations.runtime;

import io.casehub.eidos.annotations.runtime.AnnotatedAgentConfig;

public class AnnotatedOrgConfig {
    public String unitId;
    public String name;
    public String kind;
    public String kindVocabulary;
    public String parentUnit;
    public String topology;
    public String topologyVocabulary;
    public String authorityModel;
    public double selfDetermination;

    public MemberConfig[] members;
    public RelationshipConfig[] relationships;
    public AnnotatedAgentConfig.CapabilityConfig[] capabilities;
    public AnnotatedAgentConfig.GoalConfig[] goals;
    public AnnotatedAgentConfig.ConstraintConfig[] constraints;
    public GroupPolicyConfig policy;
    public RoleSpecConfig[] roleSpecs;
    public MissionSpecConfig[] missions;

    public AnnotatedOrgConfig() {}

    public static class MemberConfig {
        public String agentId;
        public String role;
        public String roleVocabulary;

        public MemberConfig() {}
    }

    public static class RelationshipConfig {
        public String source;
        public String target;
        public String kind;
        public String extendedKind;
        public String kindVocabulary;
        public String scope;
        public String scopeDomain;
        public String scopeCondition;
        public AttestationConfig attestation;

        public RelationshipConfig() {}
    }

    public static class AttestationConfig {
        public String[] dimensions;
        public String[] capabilityScope;
        public String[] signalTypes;

        public AttestationConfig() {}
    }

    public static class GroupPolicyConfig {
        public int normativePriority;
        public String normSpecificity;
        public FormationRulesConfig formationRules;

        public GroupPolicyConfig() {}
    }

    public static class FormationRulesConfig {
        public String[] allowedPaths;
        public int minGroupSize;
        public int maxGroupSize;
        public boolean allowSubGroups;
        public double proposalQuorum;

        public FormationRulesConfig() {}
    }

    public static class RoleSpecConfig {
        public String roleName;
        public String roleVocabulary;
        public int minCardinality;
        public int maxCardinality;
        public String[] incompatibleWith;
        public RolePolicyConfig[] policies;

        public RoleSpecConfig() {}
    }

    public static class RolePolicyConfig {
        public String capabilityName;
        public String kind;
        public PolicyExpectationConfig[] expectations;

        public RolePolicyConfig() {}
    }

    public static class PolicyExpectationConfig {
        public String key;
        public String value;

        public PolicyExpectationConfig() {}
    }

    public static class MissionSpecConfig {
        public String missionName;
        public String description;

        public MissionSpecConfig() {}
    }
}

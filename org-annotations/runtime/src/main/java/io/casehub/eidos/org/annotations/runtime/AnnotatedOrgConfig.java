package io.casehub.eidos.org.annotations.runtime;

import io.casehub.eidos.annotations.runtime.AnnotatedAgentConfig;

public class AnnotatedOrgConfig {
    public String unitId;
    public String name;
    public String kind;
    public String kindVocabulary;
    public String parentUnit;

    public MemberConfig[] members;
    public RelationshipConfig[] relationships;
    public AnnotatedAgentConfig.CapabilityConfig[] capabilities;
    public AnnotatedAgentConfig.GoalConfig[] goals;
    public AnnotatedAgentConfig.ConstraintConfig[] constraints;

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
}

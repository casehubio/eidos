package io.casehub.eidos.org.annotations.runtime;

public class AnnotatedOrgConfig {
    public String unitId;
    public String name;
    public String kind;
    public String kindVocabulary;
    public String parentUnit;

    public MemberConfig[] members;
    public RelationshipConfig[] relationships;

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

        public RelationshipConfig() {}
    }
}

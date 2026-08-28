package io.casehub.eidos.annotations.runtime;

public class AnnotatedAgentConfig {
    public String agentId;
    public String name;
    public String slot;
    public String provider;
    public String modelFamily;
    public String jurisdiction;
    public String dataHandlingPolicy;
    public String briefing;
    public String domainVocabulary;
    public String slotVocabulary;
    public String dispositionVocabulary;
    public String styleVocabulary;
    public String version;

    public boolean hasDisposition;
    public String socialOrient;
    public String ruleFollowing;
    public String riskAppetite;
    public String autonomy;
    public String conflictMode;
    public boolean delegation;
    public String[] dispositionProfile;
    public String[] styleProfile;

    public GoalConfig[] goals;
    public ConstraintConfig[] constraints;
    public String[] capabilities;

    public AnnotatedAgentConfig() {}

    public static class GoalConfig {
        public String name;
        public String description;
        public String priority;
        public String visibility;
        public String[] capabilities;

        public GoalConfig() {}
    }

    public static class ConstraintConfig {
        public String name;
        public String description;
        public String severity;
        public String visibility;

        public ConstraintConfig() {}
    }
}

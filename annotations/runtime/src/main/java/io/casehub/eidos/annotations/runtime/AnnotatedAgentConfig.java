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
    public String weightsFingerprint;
    public String modelVersion;

    public boolean                   hasDisposition;
    public String                    socialOrient;
    public String                    ruleFollowing;
    public String                    riskAppetite;
    public String                    autonomy;
    public String                    conflictMode;
    public boolean                   delegation;
    public DispositionWeightConfig[] dispositionProfile;
    public DispositionWeightConfig[] styleProfile;
    public String                    mbtiType;
    public String                    enneagramType;
    public AxisVocabConfig[]         axisVocabularies;

    public GoalConfig[]        goals;
    public ConstraintConfig[]  constraints;
    public String[]            capabilities;
    public CapabilityConfig[]  richCapabilities;
    public TemplateRefConfig[] templateRefs;

    public AnnotatedAgentConfig() {}

    public static class GoalConfig {
        public String   name;
        public String   description;
        public String   priority;
        public String   visibility;
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

    public static class DispositionWeightConfig {
        public String value;
        public double weight;

        public DispositionWeightConfig() {}
    }

    public static class AxisVocabConfig {
        public String axis;
        public String uri;

        public AxisVocabConfig() {}
    }

    public static class CapabilityConfig {
        public String                  name;
        public String                  description;
        public String                  capabilityVocabulary;
        public double                  qualityHint      = -1;
        public long                    latencyHintP50Ms = -1;
        public String                  costHint;
        public String[]                inputTypes;
        public String[]                outputTypes;
        public String[]                tags;
        public EpistemicDomainConfig[] epistemicDomains;
        public String[]                excludedDomains;

        public CapabilityConfig() {}
    }

    public static class EpistemicDomainConfig {
        public String value;
        public double score;

        public EpistemicDomainConfig() {}
    }

    public static class TemplateRefConfig {
        public String              id;
        public TemplateArgConfig[] args;

        public TemplateRefConfig() {}
    }

    public static class TemplateArgConfig {
        public String key;
        public String value;

        public TemplateArgConfig() {}
    }
}

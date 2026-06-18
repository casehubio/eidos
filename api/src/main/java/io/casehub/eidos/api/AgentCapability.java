package io.casehub.eidos.api;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A declared capability of an agent with operational metadata and epistemic domain qualifications.
 * qualityHint is a self-declared prior — ActorTrustScore per CapabilityTag in casehub-ledger
 * is the evidence-backed replacement that accumulates over time.
 */
public record AgentCapability(
        String name,
        Double qualityHint,
        Long latencyHintP50Ms,
        String costHint,
        List<String> inputTypes,
        List<String> outputTypes,
        List<String> tags,
        Map<String, Double> epistemicDomains,
        Set<String> excludedDomains
) {
    public AgentCapability {
        AgentDescriptorValidator.validateRequired("capability.name", name,
            AgentDescriptorValidator.MAX_CAPABILITY_NAME);
        AgentDescriptorValidator.validateOptional("costHint", costHint,
            AgentDescriptorValidator.MAX_CAPABILITY_STRING);
        AgentDescriptorValidator.validateItems("inputTypes", inputTypes,
            AgentDescriptorValidator.MAX_CAPABILITY_STRING);
        AgentDescriptorValidator.validateItems("outputTypes", outputTypes,
            AgentDescriptorValidator.MAX_CAPABILITY_STRING);
        AgentDescriptorValidator.validateItems("tags", tags,
            AgentDescriptorValidator.MAX_CAPABILITY_STRING);
        if (epistemicDomains != null) {
            AgentDescriptorValidator.validateMapKeys("epistemicDomains",
                epistemicDomains.keySet(), AgentDescriptorValidator.MAX_CAPABILITY_STRING);
        }
        if (excludedDomains != null) {
            AgentDescriptorValidator.validateItems("excludedDomains",
                excludedDomains, AgentDescriptorValidator.MAX_CAPABILITY_STRING);
            if (epistemicDomains != null) {
                excludedDomains.stream()
                    .filter(epistemicDomains::containsKey)
                    .findFirst()
                    .ifPresent(d -> { throw new AgentValidationException("excludedDomains",
                        "domain '" + d + "' appears in both excludedDomains and epistemicDomains"); });
            }
            excludedDomains = Set.copyOf(excludedDomains);
        }
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String name;
        private Double qualityHint;
        private Long latencyHintP50Ms;
        private String costHint;
        private List<String> inputTypes;
        private List<String> outputTypes;
        private List<String> tags;
        private Map<String, Double> epistemicDomains;
        private Set<String> excludedDomains;

        public Builder name(String v)                     { this.name = v; return this; }
        public Builder qualityHint(Double v)              { this.qualityHint = v; return this; }
        public Builder latencyHintP50Ms(Long v)           { this.latencyHintP50Ms = v; return this; }
        public Builder costHint(String v)                 { this.costHint = v; return this; }
        public Builder inputTypes(List<String> v)         { this.inputTypes = v; return this; }
        public Builder outputTypes(List<String> v)        { this.outputTypes = v; return this; }
        public Builder tags(List<String> v)               { this.tags = v; return this; }
        public Builder epistemicDomains(Map<String, Double> v) { this.epistemicDomains = v; return this; }
        public Builder excludedDomains(Set<String> v)     { this.excludedDomains = v; return this; }

        public AgentCapability build() {
            return new AgentCapability(name, qualityHint, latencyHintP50Ms, costHint,
                inputTypes, outputTypes, tags, epistemicDomains, excludedDomains);
        }
    }
}

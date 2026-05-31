package io.casehub.eidos.api;

import java.util.List;
import java.util.Map;

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
        Map<String, Double> epistemicDomains
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
    }
}

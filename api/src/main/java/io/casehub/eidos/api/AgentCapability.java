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
        double qualityHint,
        Long latencyHintP50Ms,
        String costHint,
        List<String> inputTypes,
        List<String> outputTypes,
        List<String> tags,
        Map<String, Double> epistemicDomains
) {}

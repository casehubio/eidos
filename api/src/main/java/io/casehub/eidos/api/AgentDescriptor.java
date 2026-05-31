package io.casehub.eidos.api;

import java.util.List;

/**
 * Structured description of an individual LLM agent across four layers:
 * identity, slot, capabilities, and disposition.
 *
 * Self-declared at registration; validated over time by peer attestations and trust scores.
 * The descriptor is a prior — evidence updates it.
 */
public record AgentDescriptor(
        String agentId,
        String name,
        String version,
        String provider,
        String modelFamily,
        String modelVersion,
        String weightsFingerprint,
        String domainVocabulary,
        String slotVocabulary,
        String dispositionVocabulary,
        String slot,
        List<AgentCapability> capabilities,
        AgentDisposition disposition,
        String jurisdiction,
        String dataHandlingPolicy,
        String tenancyId
) {
    public AgentDescriptor {
        capabilities = capabilities != null ? capabilities : List.of();
        AgentDescriptorValidator.validate(agentId, name, slot, tenancyId);
        AgentDescriptorValidator.validateOptional("version",               version,               AgentDescriptorValidator.MAX_VERSION);
        AgentDescriptorValidator.validateOptional("provider",              provider,              AgentDescriptorValidator.MAX_PROVIDER);
        AgentDescriptorValidator.validateOptional("modelFamily",           modelFamily,           AgentDescriptorValidator.MAX_PROVIDER);
        AgentDescriptorValidator.validateOptional("modelVersion",          modelVersion,          AgentDescriptorValidator.MAX_PROVIDER);
        AgentDescriptorValidator.validateOptional("weightsFingerprint",    weightsFingerprint,    AgentDescriptorValidator.MAX_WEIGHTS_FINGERPRINT);
        AgentDescriptorValidator.validateOptional("domainVocabulary",      domainVocabulary,      AgentDescriptorValidator.MAX_VOCABULARY_URI);
        AgentDescriptorValidator.validateOptional("slotVocabulary",        slotVocabulary,        AgentDescriptorValidator.MAX_VOCABULARY_URI);
        AgentDescriptorValidator.validateOptional("dispositionVocabulary", dispositionVocabulary, AgentDescriptorValidator.MAX_VOCABULARY_URI);
        AgentDescriptorValidator.validateOptional("jurisdiction",          jurisdiction,          AgentDescriptorValidator.MAX_JURISDICTION);
        AgentDescriptorValidator.validateOptional("dataHandlingPolicy",    dataHandlingPolicy,    AgentDescriptorValidator.MAX_JURISDICTION);
    }
}

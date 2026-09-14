package io.casehub.eidos.api;

/**
 * Utility for validating capability vocabularies in agent descriptors.
 */
public class CapabilityVocabularyValidator {

    /**
     * Validates all capability vocabularies in the descriptor.
     * @param descriptor agent descriptor to validate
     * @param vocabularyRegistry registry to check vocabularies against
     * @throws AgentValidationException if any vocabulary is not registered or any capability name is not a valid term
     */
    public static void validate(AgentDescriptor descriptor, VocabularyRegistry vocabularyRegistry) {
        if (descriptor.capabilities() == null) return;
        for (var cap : descriptor.capabilities()) {
            if (cap.capabilityVocabulary() != null) {
                if (!vocabularyRegistry.isRegistered(cap.capabilityVocabulary())) {
                    throw new AgentValidationException("capabilityVocabulary",
                        "vocabulary '" + cap.capabilityVocabulary() + "' is not registered");
                }
                if (vocabularyRegistry.resolve(cap.capabilityVocabulary(), cap.name()).isEmpty()) {
                    throw new AgentValidationException("capability.name",
                        "'" + cap.name() + "' is not a valid term in vocabulary '" + cap.capabilityVocabulary() + "'");
                }
            }
            if (cap.modelTier() != null) {
                String modelTierUri = MODEL_TIER_VOCABULARY_URI;
                if (vocabularyRegistry.isRegistered(modelTierUri)) {
                    if (vocabularyRegistry.resolve(modelTierUri, cap.modelTier()).isEmpty()) {
                        throw new AgentValidationException("modelTier",
                            "'" + cap.modelTier() + "' is not a valid term in vocabulary '" + modelTierUri + "'");
                    }
                }
            }
        }
    }

    static final String MODEL_TIER_VOCABULARY_URI = "urn:casehub:vocab:model-tier";

    private CapabilityVocabularyValidator() {} // prevent instantiation
}

package io.casehub.eidos.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
        Map<DispositionAxis, String> axisVocabularies,
        String slot,
        List<AgentCapability> capabilities,
        AgentDisposition disposition,
        String jurisdiction,
        String dataHandlingPolicy,
        String tenancyId,
        String briefing,
        List<TemplateRef> templates
) {
    public AgentDescriptor {
        capabilities = capabilities != null ? List.copyOf(capabilities) : List.of();
        templates    = templates != null ? List.copyOf(templates) : null;
        if (axisVocabularies != null) {
            axisVocabularies.forEach((axis, uri) ->
                                             AgentDescriptorValidator.validateRequired(
                                                     "axisVocabularies[" + axis.name() + "]", uri,
                                                     AgentDescriptorValidator.MAX_VOCABULARY_URI));
            axisVocabularies = Map.copyOf(axisVocabularies);
        }
        AgentDescriptorValidator.validate(agentId, name, slot, tenancyId);
        AgentDescriptorValidator.validateOptional("version", version, AgentDescriptorValidator.MAX_VERSION);
        AgentDescriptorValidator.validateOptional("provider", provider, AgentDescriptorValidator.MAX_PROVIDER);
        AgentDescriptorValidator.validateOptional("modelFamily", modelFamily, AgentDescriptorValidator.MAX_MODEL_IDENTIFIER);
        AgentDescriptorValidator.validateOptional("modelVersion", modelVersion, AgentDescriptorValidator.MAX_MODEL_IDENTIFIER);
        AgentDescriptorValidator.validateOptional("weightsFingerprint", weightsFingerprint, AgentDescriptorValidator.MAX_WEIGHTS_FINGERPRINT);
        AgentDescriptorValidator.validateOptional("domainVocabulary", domainVocabulary, AgentDescriptorValidator.MAX_VOCABULARY_URI);
        AgentDescriptorValidator.validateOptional("slotVocabulary", slotVocabulary, AgentDescriptorValidator.MAX_VOCABULARY_URI);
        AgentDescriptorValidator.validateOptional("dispositionVocabulary", dispositionVocabulary, AgentDescriptorValidator.MAX_VOCABULARY_URI);
        AgentDescriptorValidator.validateOptional("jurisdiction", jurisdiction, AgentDescriptorValidator.MAX_JURISDICTION);
        AgentDescriptorValidator.validateOptional("dataHandlingPolicy", dataHandlingPolicy, AgentDescriptorValidator.MAX_DATA_HANDLING_POLICY);
        AgentDescriptorValidator.validateOptional("briefing", briefing, AgentDescriptorValidator.MAX_BRIEFING, 0x000A);
    }

    public Optional<String> vocabUriForSlot() {
        if (slotVocabulary != null) {return Optional.of(slotVocabulary);}
        if (domainVocabulary != null) {return Optional.of(domainVocabulary);}
        return Optional.empty();
    }

    public Optional<String> vocabUriForAxis(final DispositionAxis axis) {
        if (axisVocabularies != null) {
            String uri = axisVocabularies.get(axis);
            if (uri != null) {return Optional.of(uri);}
        }
        if (dispositionVocabulary != null) {return Optional.of(dispositionVocabulary);}
        if (domainVocabulary != null) {return Optional.of(domainVocabulary);}
        return Optional.empty();
    }

    public static Builder builder() {return new Builder();}

    public static final class Builder {
        private String agentId, name, version, provider,
                modelFamily, modelVersion, weightsFingerprint,
                domainVocabulary, slotVocabulary, dispositionVocabulary;
        private Map<DispositionAxis, String> axisVocabularies;
        private String                       slot;
        private List<AgentCapability>        capabilities = List.of();
        private AgentDisposition             disposition;
        private String                       jurisdiction, dataHandlingPolicy, tenancyId, briefing;
        private List<TemplateRef> templates;

        public Builder agentId(String v)                                {
                                                                            this.agentId = v;
                                                                            return this;
                                                                        }

        public Builder name(String v)                                   {
                                                                            this.name = v;
                                                                            return this;
                                                                        }

        public Builder version(String v)                                {
                                                                            this.version = v;
                                                                            return this;
                                                                        }

        public Builder provider(String v)                               {
                                                                            this.provider = v;
                                                                            return this;
                                                                        }

        public Builder modelFamily(String v)                            {
                                                                            this.modelFamily = v;
                                                                            return this;
                                                                        }

        public Builder modelVersion(String v)                           {
                                                                            this.modelVersion = v;
                                                                            return this;
                                                                        }

        public Builder weightsFingerprint(String v)                     {
                                                                            this.weightsFingerprint = v;
                                                                            return this;
                                                                        }

        public Builder domainVocabulary(String v)                       {
                                                                            this.domainVocabulary = v;
                                                                            return this;
                                                                        }

        public Builder slotVocabulary(String v)                         {
                                                                            this.slotVocabulary = v;
                                                                            return this;
                                                                        }

        public Builder dispositionVocabulary(String v)                  {
                                                                            this.dispositionVocabulary = v;
                                                                            return this;
                                                                        }

        public Builder axisVocabularies(Map<DispositionAxis, String> v) {
                                                                            this.axisVocabularies = v;
                                                                            return this;
                                                                        }

        public Builder slot(String v)                                   {
                                                                            this.slot = v;
                                                                            return this;
                                                                        }

        public Builder capabilities(List<AgentCapability> v)            {
                                                                            this.capabilities = v;
                                                                            return this;
                                                                        }

        public Builder disposition(AgentDisposition v)                  {
                                                                            this.disposition = v;
                                                                            return this;
                                                                        }

        public Builder jurisdiction(String v)                           {
                                                                            this.jurisdiction = v;
                                                                            return this;
                                                                        }

        public Builder dataHandlingPolicy(String v)                     {
                                                                            this.dataHandlingPolicy = v;
                                                                            return this;
                                                                        }

        public Builder tenancyId(String v)                              {
                                                                            this.tenancyId = v;
                                                                            return this;
                                                                        }

        public Builder briefing(String v)                               {
                                                                            this.briefing = v;
                                                                            return this;
                                                                        }

        public Builder templates(List<TemplateRef> v)                   {
                                                                            this.templates = v;
                                                                            return this;
                                                                        }

        public AgentDescriptor build() {
            return new AgentDescriptor(
                    agentId, name, version, provider,
                    modelFamily, modelVersion, weightsFingerprint,
                    domainVocabulary, slotVocabulary, dispositionVocabulary,
                    axisVocabularies, slot, capabilities, disposition,
                    jurisdiction, dataHandlingPolicy, tenancyId, briefing,
                    templates);
        }
    }
}

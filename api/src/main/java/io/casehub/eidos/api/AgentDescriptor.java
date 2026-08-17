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
        String styleVocabulary,
        Map<DispositionAxis, String> axisVocabularies,
        String slot,
        List<AgentCapability> capabilities,
        AgentDisposition disposition,
        String jurisdiction,
        String dataHandlingPolicy,
        String tenancyId,
        String briefing,
        List<TemplateRef> templates,
        List<AgentGoal> goals,
        List<AgentConstraint> constraints
) {
    public AgentDescriptor {
        capabilities = capabilities != null ? List.copyOf(capabilities) : List.of();
        templates    = templates != null ? List.copyOf(templates) : null;
        goals        = goals != null ? List.copyOf(goals) : List.of();
        constraints  = constraints != null ? List.copyOf(constraints) : List.of();
        if (capabilities.size() > AgentDescriptorValidator.MAX_CAPABILITIES) {
            throw new AgentValidationException("capabilities",
                "exceeds maximum count " + AgentDescriptorValidator.MAX_CAPABILITIES + " (was " + capabilities.size() + ")");
        }
        if (capabilities.size() > 1) {
            long distinctNames = capabilities.stream().map(AgentCapability::name).distinct().count();
            if (distinctNames < capabilities.size()) {
                String dup = capabilities.stream().map(AgentCapability::name)
                    .collect(java.util.stream.Collectors.groupingBy(n -> n, java.util.stream.Collectors.counting()))
                    .entrySet().stream().filter(e -> e.getValue() > 1).map(java.util.Map.Entry::getKey)
                    .findFirst().orElse("?");
                throw new AgentValidationException("capabilities", "duplicate capability name: " + dup);
            }
        }
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
        if (goals.size() > AgentDescriptorValidator.MAX_GOALS) {
            throw new AgentValidationException("goals",
                                               "exceeds maximum count " + AgentDescriptorValidator.MAX_GOALS + " (was " + goals.size() + ")");
        }
        if (constraints.size() > AgentDescriptorValidator.MAX_CONSTRAINTS) {
            throw new AgentValidationException("constraints",
                                               "exceeds maximum count " + AgentDescriptorValidator.MAX_CONSTRAINTS + " (was " + constraints.size() + ")");
        }
        if (goals.size() > 1) {
            long distinctNames = goals.stream().map(AgentGoal::name).distinct().count();
            if (distinctNames < goals.size()) {
                String dup = goals.stream().map(AgentGoal::name)
                                  .collect(java.util.stream.Collectors.groupingBy(n -> n, java.util.stream.Collectors.counting()))
                                  .entrySet().stream().filter(e -> e.getValue() > 1).map(java.util.Map.Entry::getKey)
                                  .findFirst().orElse("?");
                throw new AgentValidationException("goals", "duplicate goal name: " + dup);
            }
        }
        if (constraints.size() > 1) {
            long distinctNames = constraints.stream().map(AgentConstraint::name).distinct().count();
            if (distinctNames < constraints.size()) {
                String dup = constraints.stream().map(AgentConstraint::name)
                                        .collect(java.util.stream.Collectors.groupingBy(n -> n, java.util.stream.Collectors.counting()))
                                        .entrySet().stream().filter(e -> e.getValue() > 1).map(java.util.Map.Entry::getKey)
                                        .findFirst().orElse("?");
                throw new AgentValidationException("constraints", "duplicate constraint name: " + dup);
            }
        }
        if (!goals.isEmpty() && !capabilities.isEmpty()) {
            var capabilityNames = capabilities.stream()
                .map(AgentCapability::name).collect(java.util.stream.Collectors.toSet());
            for (var goal : goals) {
                for (var capName : goal.capabilities()) {
                    if (!capabilityNames.contains(capName)) {
                        throw new AgentValidationException("goals",
                            "goal '" + goal.name() + "' references unknown capability '" + capName + "'");
                    }
                }
            }
        }
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

    public List<AgentGoal> publicGoals() {
        return goals.stream().filter(g -> g.visibility() == Visibility.PUBLIC).toList();
    }

    public List<AgentConstraint> publicConstraints() {
        return constraints.stream().filter(c -> c.visibility() == Visibility.PUBLIC).toList();
    }

    public boolean hasGoal(String name) {
        return goals.stream().anyMatch(g -> g.name().equals(name));
    }

    public boolean hasConstraint(String name) {
        return constraints.stream().anyMatch(c -> c.name().equals(name));
    }


    public static Builder builder() {return new Builder();}

    public Builder toBuilder() {
        return new Builder()
                .agentId(this.agentId).name(this.name).version(this.version)
                .provider(this.provider).modelFamily(this.modelFamily)
                .modelVersion(this.modelVersion).weightsFingerprint(this.weightsFingerprint)
                .domainVocabulary(this.domainVocabulary).slotVocabulary(this.slotVocabulary)
                .dispositionVocabulary(this.dispositionVocabulary)
                .styleVocabulary(this.styleVocabulary)
                .axisVocabularies(this.axisVocabularies)
                .slot(this.slot).capabilities(this.capabilities)
                .disposition(this.disposition).jurisdiction(this.jurisdiction)
                .dataHandlingPolicy(this.dataHandlingPolicy)
                .tenancyId(this.tenancyId).briefing(this.briefing)
                .templates(this.templates).goals(this.goals)
                .constraints(this.constraints);
    }

    public static final class Builder {
        private String agentId, name, version, provider,
                modelFamily, modelVersion, weightsFingerprint,
                domainVocabulary, slotVocabulary, dispositionVocabulary,
                styleVocabulary;
        private Map<DispositionAxis, String> axisVocabularies;
        private String                       slot;
        private List<AgentCapability>        capabilities = List.of();
        private AgentDisposition             disposition;
        private String                       jurisdiction, dataHandlingPolicy, tenancyId, briefing;
        private List<TemplateRef> templates;
        private List<AgentGoal>       goals;
        private List<AgentConstraint> constraints;

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

        public Builder styleVocabulary(String v)                        {
                                                                            this.styleVocabulary = v;
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

        public Builder goals(List<AgentGoal> v)                         {
                                                                            this.goals = v;
                                                                            return this;
                                                                        }

        public Builder constraints(List<AgentConstraint> v)             {
                                                                            this.constraints = v;
                                                                            return this;
                                                                        }

        public AgentDescriptor build() {
            return new AgentDescriptor(
                    agentId, name, version, provider,
                    modelFamily, modelVersion, weightsFingerprint,
                    domainVocabulary, slotVocabulary, dispositionVocabulary,
                    styleVocabulary,
                    axisVocabularies, slot, capabilities, disposition,
                    jurisdiction, dataHandlingPolicy, tenancyId, briefing,
                    templates,
                    goals, constraints);
        }
    }
}

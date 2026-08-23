package io.casehub.eidos.runtime.registrar;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.casehub.eidos.api.AgentCapability;
import io.casehub.eidos.api.AgentConstraint;
import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentDisposition;
import io.casehub.eidos.api.AgentGoal;
import io.casehub.eidos.api.ConstraintSeverity;
import io.casehub.eidos.api.DispositionAxis;
import io.casehub.eidos.api.GoalPriority;
import io.casehub.eidos.api.TemplateRef;
import io.casehub.eidos.api.Visibility;
import io.casehub.eidos.api.VocabularyRegistry;
import io.casehub.eidos.api.spi.AgentDescriptorRegistrar;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class ClasspathYamlDescriptorRegistrar implements AgentDescriptorRegistrar {

    private static final String       RESOURCE_PATH = "META-INF/eidos/descriptors.yaml";
    private static final ObjectMapper YAML_MAPPER   = new ObjectMapper(new YAMLFactory())
                                                              .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

    private static AgentDescriptor toDescriptor(final DescriptorConfig cfg, final VocabularyRegistry vocabRegistry) {
        final var builder = AgentDescriptor.builder()
                                           .agentId(cfg.agentId).name(cfg.name).slot(cfg.slot).tenancyId(cfg.tenancyId)
                                           .version(cfg.version).provider(cfg.provider)
                                           .modelFamily(cfg.modelFamily).modelVersion(cfg.modelVersion)
                                           .weightsFingerprint(cfg.weightsFingerprint)
                                           .domainVocabulary(cfg.domainVocabulary)
                                           .slotVocabulary(cfg.slotVocabulary)
                                           .dispositionVocabulary(cfg.dispositionVocabulary)
                                           .styleVocabulary(cfg.styleVocabulary)
                                           .jurisdiction(cfg.jurisdiction)
                                           .dataHandlingPolicy(cfg.dataHandlingPolicy)
                                           .briefing(cfg.briefing);

        if (cfg.axisVocabularies != null && !cfg.axisVocabularies.isEmpty()) {
            final var axisMap = new LinkedHashMap<DispositionAxis, String>();
            cfg.axisVocabularies.forEach((key, uri) -> axisMap.put(DispositionAxis.valueOf(key), uri));
            builder.axisVocabularies(axisMap);
        }

        if (cfg.disposition != null) {
            var dispBuilder = AgentDisposition.builder()
                    .socialOrient(cfg.disposition.socialOrient)
                    .ruleFollowing(cfg.disposition.ruleFollowing)
                    .riskAppetite(cfg.disposition.riskAppetite)
                    .autonomy(cfg.disposition.autonomy)
                    .conflictMode(cfg.disposition.conflictMode)
                    .delegation(cfg.disposition.delegation);

            if (cfg.disposition.dispositionProfile != null && !cfg.disposition.dispositionProfile.isEmpty()) {
                dispBuilder.dispositionProfile(cfg.disposition.dispositionProfile.stream()
                        .map(dv -> new io.casehub.eidos.api.DispositionValue(dv.term, dv.weight))
                        .toList());
            } else if (cfg.disposition.mbtiType != null && vocabRegistry != null) {
                vocabRegistry.resolve("urn:casehub:vocab:mbti", cfg.disposition.mbtiType.toLowerCase(java.util.Locale.ROOT))
                        .ifPresent(term -> dispBuilder.dispositionProfile(term.defaultProfile()));
            }

            if (cfg.disposition.enneagramType != null && vocabRegistry != null) {
                var enneaValue = cfg.disposition.enneagramType.toLowerCase(java.util.Locale.ROOT);
                if (vocabRegistry.resolve("urn:casehub:vocab:enneagram", enneaValue).isPresent()) {
                    for (var axis : DispositionAxis.values()) {
                        String conscientiousnessMatch = vocabRegistry.equivalentValues(
                                "urn:casehub:vocab:enneagram", enneaValue,
                                "urn:casehub:vocab:conscientiousness", axis).orElse(null);
                        if (conscientiousnessMatch != null) {
                            switch (axis) {
                                case SOCIAL_ORIENTATION -> { if (cfg.disposition.socialOrient == null) dispBuilder.socialOrient(conscientiousnessMatch); }
                                case RULE_FOLLOWING     -> { if (cfg.disposition.ruleFollowing == null) dispBuilder.ruleFollowing(conscientiousnessMatch); }
                                case RISK_APPETITE      -> { if (cfg.disposition.riskAppetite == null) dispBuilder.riskAppetite(conscientiousnessMatch); }
                                case AUTONOMY           -> { if (cfg.disposition.autonomy == null) dispBuilder.autonomy(conscientiousnessMatch); }
                                case CONFLICT_MODE      -> {}
                            }
                        }
                        if (axis == DispositionAxis.CONFLICT_MODE && cfg.disposition.conflictMode == null) {
                            vocabRegistry.equivalentValues(
                                    "urn:casehub:vocab:enneagram", enneaValue,
                                    "urn:casehub:vocab:thomas-kilmann", axis)
                                    .ifPresent(dispBuilder::conflictMode);
                        }
                    }
                }
            }

            if (cfg.disposition.styleProfile != null && !cfg.disposition.styleProfile.isEmpty()) {
                dispBuilder.styleProfile(cfg.disposition.styleProfile.stream()
                        .map(dv -> new io.casehub.eidos.api.DispositionValue(dv.term, dv.weight))
                        .toList());
            }

            builder.disposition(dispBuilder.build());
        }

        if (cfg.capabilities != null) {
            builder.capabilities(cfg.capabilities.stream().map(c ->
                                                                       new AgentCapability(c.name, c.description, c.capabilityVocabulary, c.qualityHint, c.latencyHintP50Ms, c.costHint,
                                                                                           c.inputTypes, c.outputTypes, c.tags, c.epistemicDomains, c.excludedDomains)
                                                              ).toList());
        }

        if (cfg.templates != null) {
            builder.templates(cfg.templates.stream()
                                           .map(tr -> new TemplateRef(tr.ref, tr.args))
                                           .toList());
        }

        if (cfg.goals != null) {
            builder.goals(cfg.goals.stream().map(g ->
                new AgentGoal(g.name, g.description, g.priority, g.visibility, g.capabilities, null)
            ).toList());
        }

        if (cfg.constraints != null) {
            builder.constraints(cfg.constraints.stream().map(c ->
                new AgentConstraint(c.name, c.description, c.visibility, c.severity)
            ).toList());
        }

        return builder.build();
    }

    @Override
    public List<AgentDescriptor> descriptors() {
        final Enumeration<URL> urls;
        try {
            urls = Thread.currentThread().getContextClassLoader().getResources(RESOURCE_PATH);
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to scan classpath for " + RESOURCE_PATH, e);
        }

        final var all = new ArrayList<AgentDescriptor>();
        while (urls.hasMoreElements()) {
            final var url = urls.nextElement();
            try (final var stream = url.openStream()) {
                all.addAll(loadFrom(stream));
            } catch (final Exception e) {
                throw new IllegalStateException(
                        "Failed to load descriptors from " + url + ": " + e.getMessage(), e);
            }
        }
        return List.copyOf(all);
    }

    List<AgentDescriptor> loadFrom(final InputStream yaml) {
        return loadFrom(yaml, null);
    }

    public List<AgentDescriptor> loadFrom(final InputStream yaml, final VocabularyRegistry vocabRegistry) {
        if (yaml == null) {return List.of();}
        final DescriptorFile file;
        try {
            file = YAML_MAPPER.readValue(yaml, DescriptorFile.class);
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to parse YAML: " + e.getMessage(), e);
        }
        if (file.descriptors == null || file.descriptors.isEmpty()) {return List.of();}

        final var result = new ArrayList<AgentDescriptor>(file.descriptors.size());
        for (final var cfg : file.descriptors) {
            result.add(toDescriptor(cfg, vocabRegistry));
        }
        return result;
    }


    static class DescriptorFile {
        public List<DescriptorConfig> descriptors;
    }

    static class DescriptorConfig {
        public String agentId, name, slot, tenancyId, version, provider,
                modelFamily, modelVersion, weightsFingerprint,
                domainVocabulary, slotVocabulary, dispositionVocabulary,
                styleVocabulary,
                jurisdiction, dataHandlingPolicy, briefing;
        public Map<String, String>     axisVocabularies;
        public DispositionConfig       disposition;
        public List<CapabilityConfig>  capabilities;
        public List<TemplateRefConfig> templates;
        public List<GoalConfig> goals;
        public List<ConstraintConfig> constraints;
    }

    static class DispositionConfig {
        public String socialOrient, ruleFollowing, riskAppetite, autonomy, conflictMode;
        public boolean                      delegation;
        public String                       mbtiType;
        public String                       enneagramType;
        public List<DispositionValueConfig> dispositionProfile;
        public List<DispositionValueConfig> styleProfile;
    }

    static class DispositionValueConfig {
        public String term;
        public double weight;
    }

    static class CapabilityConfig {
        public String       name;
        public String       description;
        public String       capabilityVocabulary;
        public Double       qualityHint;
        public Long         latencyHintP50Ms;
        public String       costHint;
        public List<String> inputTypes, outputTypes, tags;
        public Map<String, Double> epistemicDomains;
        public Set<String>         excludedDomains;
    }

    static class TemplateRefConfig {
        public String              ref;
        public Map<String, String> args;
    }

    static class GoalConfig {
        public String       name;
        public String       description;
        public GoalPriority priority;
        public Visibility   visibility;
        public List<String> capabilities;
    }

    static class ConstraintConfig {
        public String             name;
        public String             description;
        public Visibility         visibility;
        public ConstraintSeverity severity;
    }

}

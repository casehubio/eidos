package io.casehub.eidos.annotations.runtime;

import io.casehub.eidos.api.*;
import io.casehub.eidos.api.spi.AgentDescriptorRegistrar;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.runtime.annotations.Recorder;
import org.eclipse.microprofile.config.ConfigProvider;

import java.util.*;
import java.util.function.Function;

@Recorder
public class EidosAnnotationsRecorder {

    private static final String TENANCY_CONFIG_KEY = "casehub.eidos.annotations.default-tenancy-id";

    public Function<SyntheticCreationalContext<AgentDescriptorRegistrar>, AgentDescriptorRegistrar>
    createRegistrar(AnnotatedAgentConfig config) {
        return ctx -> {
            var vocabRegistry = ctx.getInjectedReference(VocabularyRegistry.class);
            var tenancyId = ConfigProvider.getConfig()
                                          .getOptionalValue(TENANCY_CONFIG_KEY, String.class)
                                          .orElse("default");

            var builder = AgentDescriptor.builder()
                                         .agentId(config.agentId).name(config.name).slot(config.slot).tenancyId(tenancyId);

            if (notEmpty(config.provider)) {builder.provider(config.provider);}
            if (notEmpty(config.modelFamily)) {builder.modelFamily(config.modelFamily);}
            if (notEmpty(config.jurisdiction)) {builder.jurisdiction(config.jurisdiction);}
            if (notEmpty(config.dataHandlingPolicy)) {builder.dataHandlingPolicy(config.dataHandlingPolicy);}
            if (notEmpty(config.briefing)) {builder.briefing(config.briefing);}
            if (notEmpty(config.domainVocabulary)) {builder.domainVocabulary(config.domainVocabulary);}
            if (notEmpty(config.slotVocabulary)) {builder.slotVocabulary(config.slotVocabulary);}
            if (notEmpty(config.dispositionVocabulary)) {builder.dispositionVocabulary(config.dispositionVocabulary);}
            if (notEmpty(config.styleVocabulary)) {builder.styleVocabulary(config.styleVocabulary);}
            if (notEmpty(config.version)) {builder.version(config.version);}
            if (notEmpty(config.weightsFingerprint)) {builder.weightsFingerprint(config.weightsFingerprint);}
            if (notEmpty(config.modelVersion)) {builder.modelVersion(config.modelVersion);}

            if (config.hasDisposition) {
                var db = AgentDisposition.builder().delegation(config.delegation);
                if (notEmpty(config.socialOrient)) {db.socialOrient(config.socialOrient);}
                if (notEmpty(config.ruleFollowing)) {db.ruleFollowing(config.ruleFollowing);}
                if (notEmpty(config.riskAppetite)) {db.riskAppetite(config.riskAppetite);}
                if (notEmpty(config.autonomy)) {db.autonomy(config.autonomy);}
                if (notEmpty(config.conflictMode)) {db.conflictMode(config.conflictMode);}

                if (config.dispositionProfile != null && config.dispositionProfile.length > 0) {
                    var values = new ArrayList<DispositionValue>();
                    for (var dp : config.dispositionProfile) {
                        if (notEmpty(dp.value)) {values.add(new DispositionValue(dp.value, dp.weight));}
                    }
                    if (!values.isEmpty()) {db.dispositionProfile(values);}
                }
                if (config.styleProfile != null && config.styleProfile.length > 0) {
                    var values = new ArrayList<DispositionValue>();
                    for (var sp : config.styleProfile) {
                        if (notEmpty(sp.value)) {values.add(new DispositionValue(sp.value, sp.weight));}
                    }
                    if (!values.isEmpty()) {db.styleProfile(values);}
                }

                var explicitAxes = new EnumMap<DispositionAxis, String>(DispositionAxis.class);
                if (notEmpty(config.socialOrient)) {
                    explicitAxes.put(DispositionAxis.SOCIAL_ORIENTATION, config.socialOrient);
                }
                if (notEmpty(config.ruleFollowing)) {
                    explicitAxes.put(DispositionAxis.RULE_FOLLOWING, config.ruleFollowing);
                }
                if (notEmpty(config.riskAppetite)) {
                    explicitAxes.put(DispositionAxis.RISK_APPETITE, config.riskAppetite);
                }
                if (notEmpty(config.autonomy)) {explicitAxes.put(DispositionAxis.AUTONOMY, config.autonomy);}
                if (notEmpty(config.conflictMode)) {
                    explicitAxes.put(DispositionAxis.CONFLICT_MODE, config.conflictMode);
                }

                PersonalityTypeDeriver.derive(
                        new PersonalityInput(
                                config.mbtiType != null ? config.mbtiType : "",
                                config.enneagramType != null ? config.enneagramType : "",
                                config.dispositionProfile != null && config.dispositionProfile.length > 0,
                                explicitAxes),
                        vocabRegistry, db);

                builder.disposition(db.build());
            }

            if (config.axisVocabularies != null && config.axisVocabularies.length > 0) {
                var map = new EnumMap<DispositionAxis, String>(DispositionAxis.class);
                for (var av : config.axisVocabularies) {
                    map.put(DispositionAxis.valueOf(av.axis), av.uri);
                }
                builder.axisVocabularies(map);
            }

            if (config.goals != null) {
                var goals = new ArrayList<AgentGoal>();
                for (var g : config.goals) {
                    Map<String, String> attrs = null;
                    if (g.attributes != null && g.attributes.length > 0) {
                        attrs = new HashMap<>();
                        for (var a : g.attributes) {attrs.put(a.key, a.value);}
                    }
                    goals.add(new AgentGoal(g.name, g.description,
                                            GoalPriority.valueOf(g.priority), Visibility.valueOf(g.visibility),
                                            g.capabilities != null ? List.of(g.capabilities) : List.of(), attrs));
                }
                builder.goals(goals);
            }

            if (config.constraints != null) {
                var constraints = new ArrayList<AgentConstraint>();
                for (var c : config.constraints) {
                    constraints.add(new AgentConstraint(c.name, c.description,
                                                        Visibility.valueOf(c.visibility), ConstraintSeverity.valueOf(c.severity)));
                }
                builder.constraints(constraints);
            }

            buildCapabilities(config, builder);
            buildTemplates(config, builder);

            return (AgentDescriptorRegistrar) () -> List.of(builder.build());
        };
    }

    private static void buildCapabilities(AnnotatedAgentConfig config, AgentDescriptor.Builder builder) {
        var caps = new ArrayList<AgentCapability>();
        if (config.capabilities != null && config.capabilities.length > 0) {
            for (var name : config.capabilities) {
                caps.add(AgentCapability.builder().name(name).build());
            }
        }
        if (config.richCapabilities != null) {
            for (var cap : config.richCapabilities) {
                var cb = AgentCapability.builder().name(cap.name);
                if (notEmpty(cap.description)) {cb.description(cap.description);}
                if (notEmpty(cap.capabilityVocabulary)) {cb.capabilityVocabulary(cap.capabilityVocabulary);}
                if (cap.qualityHint >= 0) {cb.qualityHint(cap.qualityHint);}
                if (cap.latencyHintP50Ms >= 0) {cb.latencyHintP50Ms(cap.latencyHintP50Ms);}
                if (notEmpty(cap.costHint)) {cb.costHint(cap.costHint);}
                if (cap.inputTypes != null && cap.inputTypes.length > 0) {cb.inputTypes(List.of(cap.inputTypes));}
                if (cap.outputTypes != null && cap.outputTypes.length > 0) {cb.outputTypes(List.of(cap.outputTypes));}
                if (cap.tags != null && cap.tags.length > 0) {cb.tags(List.of(cap.tags));}
                if (cap.epistemicDomains != null && cap.epistemicDomains.length > 0) {
                    var map = new HashMap<String, Double>();
                    for (var ed : cap.epistemicDomains) {map.put(ed.value, ed.score);}
                    cb.epistemicDomains(map);
                }
                if (cap.excludedDomains != null && cap.excludedDomains.length > 0) {
                    cb.excludedDomains(Set.of(cap.excludedDomains));
                }
                caps.add(cb.build());
            }
        }
        if (!caps.isEmpty()) {builder.capabilities(caps);}
    }

    private static void buildTemplates(AnnotatedAgentConfig config, AgentDescriptor.Builder builder) {
        if (config.templateRefs == null || config.templateRefs.length == 0) {return;}
        var refs = new ArrayList<TemplateRef>();
        for (var ref : config.templateRefs) {
            var args = new HashMap<String, String>();
            if (ref.args != null) {
                for (var arg : ref.args) {args.put(arg.key, arg.value);}
            }
            refs.add(new TemplateRef(ref.id, args));
        }
        builder.templates(refs);
    }

    private static boolean notEmpty(String s) {
        return s != null && !s.isEmpty();
    }
}

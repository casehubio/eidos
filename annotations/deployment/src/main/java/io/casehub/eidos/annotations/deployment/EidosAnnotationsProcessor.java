package io.casehub.eidos.annotations.deployment;

import io.casehub.eidos.annotations.AgentCapabilities;
import io.casehub.eidos.annotations.AgentCapabilityDef;
import io.casehub.eidos.annotations.AgentConstraints;
import io.casehub.eidos.annotations.AgentGoals;
import io.casehub.eidos.annotations.AgentTemplateRef;
import io.casehub.eidos.annotations.AgentTemplates;
import io.casehub.eidos.annotations.Disposition;
import io.casehub.eidos.annotations.Identity;
import io.casehub.eidos.annotations.NameDerivation;
import io.casehub.eidos.annotations.runtime.AnnotatedAgentConfig;
import io.casehub.eidos.annotations.runtime.EidosAnnotationsRecorder;
import io.casehub.eidos.api.Discoverable;
import io.casehub.eidos.api.spi.AgentDescriptorRegistrar;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

class EidosAnnotationsProcessor {
    private static final org.jboss.logging.Logger LOG = org.jboss.logging.Logger.getLogger(EidosAnnotationsProcessor.class);


    private static final String FEATURE = "eidos-annotations";
    private static final DotName IDENTITY = DotName.createSimple(Identity.class);
    private static final DotName DISPOSITION = DotName.createSimple(Disposition.class);
    private static final DotName AGENT_GOALS = DotName.createSimple(AgentGoals.class);
    private static final DotName AGENT_CONSTRAINTS = DotName.createSimple(AgentConstraints.class);
    private static final DotName DISCOVERABLE = DotName.createSimple(Discoverable.class);
    private static final DotName AGENT_CAPABILITY_DEF = DotName.createSimple(AgentCapabilityDef.class);
    private static final DotName AGENT_CAPABILITIES = DotName.createSimple(AgentCapabilities.class);
    private static final DotName AGENT_TEMPLATE_REF = DotName.createSimple(AgentTemplateRef.class);
    private static final DotName AGENT_TEMPLATES = DotName.createSimple(AgentTemplates.class);

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    EidosAnnotationProcessedBuildItem processAnnotations(
            EidosAnnotationsRecorder recorder,
            CombinedIndexBuildItem index,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {

        var identityAnnotations = index.getIndex().getAnnotations(IDENTITY);
        if (identityAnnotations.isEmpty()) {
            return new EidosAnnotationProcessedBuildItem(Set.of());
        }

        var processedClasses = new HashSet<String>();
        var derivedIds = new HashMap<String, String>();

        for (var annotation : identityAnnotations) {
            if (annotation.target().kind() != AnnotationTarget.Kind.CLASS) continue;
            var classInfo = annotation.target().asClass();
            var className = classInfo.name().toString();
            processedClasses.add(className);

            var config = extractConfig(annotation, classInfo, index, derivedIds);

            syntheticBeans.produce(SyntheticBeanBuildItem
                    .configure(AgentDescriptorRegistrar.class)
                    .scope(ApplicationScoped.class)
                    .identifier("eidos-ann-" + className)
                    .setRuntimeInit()
                    .createWith(recorder.createRegistrar(config))
                    .addInjectionPoint(org.jboss.jandex.ClassType.create(DotName.createSimple(io.casehub.eidos.api.VocabularyRegistry.class)))
                    .done());
        }

        validateVocabularyTerms(index);
        warnDiscoverableWithoutIdentity(index, processedClasses);

        return new EidosAnnotationProcessedBuildItem(processedClasses);
    }

    private void validateVocabularyTerms(CombinedIndexBuildItem index) {
        var vocabMetaDotName = DotName.createSimple("io.casehub.eidos.api.VocabularyMetadata");
        var vocabAnnotations = index.getIndex().getAnnotations(vocabMetaDotName);
        if (vocabAnnotations.isEmpty()) {return;}

        var vocabs = new HashMap<String, java.util.Set<String>>();
        for (var va : vocabAnnotations) {
            if (va.target().kind() != AnnotationTarget.Kind.CLASS) {continue;}
            var uri       = va.value("uri").asString();
            var enumClass = va.target().asClass();
            var terms     = new HashSet<String>();
            for (var field : enumClass.fields()) {
                if (field.isEnumConstant()) {terms.add(field.name());}
            }
            vocabs.put(uri, terms);
        }

        for (var identityAnn : index.getIndex().getAnnotations(IDENTITY)) {
            if (identityAnn.target().kind() != AnnotationTarget.Kind.CLASS) {continue;}
            var classInfo         = identityAnn.target().asClass();
            var vocabUri          = stringValue(identityAnn, "vocabulary");
            var dispVocabUri      = stringValue(identityAnn, "dispositionVocabulary");
            var styleVocabUri     = stringValue(identityAnn, "styleVocabulary");
            var effectiveDispUri  = !dispVocabUri.isEmpty() ? dispVocabUri : vocabUri;
            var effectiveStyleUri = !styleVocabUri.isEmpty() ? styleVocabUri : vocabUri;

            var dispAnn = classInfo.annotation(DISPOSITION);
            if (dispAnn == null) {continue;}

            if (!effectiveDispUri.isEmpty()) {
                var dispTerms = vocabs.get(effectiveDispUri);
                if (dispTerms != null) {
                    validateTerm(dispAnn, "socialOrient", dispTerms, effectiveDispUri, classInfo);
                    validateTerm(dispAnn, "ruleFollowing", dispTerms, effectiveDispUri, classInfo);
                    validateTerm(dispAnn, "riskAppetite", dispTerms, effectiveDispUri, classInfo);
                    validateTerm(dispAnn, "autonomy", dispTerms, effectiveDispUri, classInfo);
                    validateTerm(dispAnn, "conflictMode", dispTerms, effectiveDispUri, classInfo);
                    validateArrayTerms(dispAnn, "dispositionProfile", dispTerms, effectiveDispUri, classInfo);
                }
            }
            if (!effectiveStyleUri.isEmpty()) {
                var styleTerms = vocabs.get(effectiveStyleUri);
                if (styleTerms != null) {
                    validateArrayTerms(dispAnn, "styleProfile", styleTerms, effectiveStyleUri, classInfo);
                }
            }
        }
    }

    private void warnDiscoverableWithoutIdentity(CombinedIndexBuildItem index, Set<String> processedClasses) {
        for (var ann : index.getIndex().getAnnotations(DISCOVERABLE)) {
            if (ann.target().kind() != AnnotationTarget.Kind.CLASS) {continue;}
            var className = ann.target().asClass().name().toString();
            if (!processedClasses.contains(className)) {
                LOG.warnf("Class %s has @Discoverable but no @Identity — capabilities will not be registered", className);
            }
        }
        for (var dotName : java.util.List.of(AGENT_CAPABILITY_DEF, AGENT_CAPABILITIES)) {
            for (var ann : index.getIndex().getAnnotations(dotName)) {
                if (ann.target().kind() != AnnotationTarget.Kind.CLASS) {continue;}
                var className = ann.target().asClass().name().toString();
                if (!processedClasses.contains(className)) {
                    LOG.warnf("Class %s has @AgentCapabilityDef but no @Identity — capabilities will not be registered", className);
                }
            }
        }
        for (var dotName : java.util.List.of(AGENT_TEMPLATE_REF, AGENT_TEMPLATES)) {
            for (var ann : index.getIndex().getAnnotations(dotName)) {
                if (ann.target().kind() != AnnotationTarget.Kind.CLASS) {continue;}
                var className = ann.target().asClass().name().toString();
                if (!processedClasses.contains(className)) {
                    LOG.warnf("Class %s has @AgentTemplateRef but no @Identity — templates will not be registered", className);
                }
            }
        }
    }


    private void validateTerm(AnnotationInstance ann, String field,
                              java.util.Set<String> validTerms, String vocabUri, ClassInfo classInfo) {
        var term = stringValue(ann, field);
        if (term.isEmpty()) {return;}
        if (validTerms.stream().noneMatch(t -> t.equalsIgnoreCase(term))) {
            LOG.warnf("@Disposition.%s value '%s' on %s may not be a valid term in vocabulary '%s'"
                      + " (build-time check uses enum constant names, not VocabularyTerm.value())",
                      field, term, classInfo.name(), vocabUri);
        }
    }

    private void validateArrayTerms(AnnotationInstance ann, String field,
                                    java.util.Set<String> validTerms, String vocabUri, ClassInfo classInfo) {
        var v = ann.value(field);
        if (v == null) {return;}
        if (field.equals("dispositionProfile") || field.equals("styleProfile")) {
            for (var nested : v.asNestedArray()) {
                var term = nested.value("value").asString();
                if (term.isEmpty()) {continue;}
                if (validTerms.stream().noneMatch(t -> t.equalsIgnoreCase(term))) {
                    LOG.warnf("@Disposition.%s value '%s' on %s may not be a valid term in vocabulary '%s'"
                              + " (build-time check uses enum constant names, not VocabularyTerm.value())",
                              field, term, classInfo.name(), vocabUri);
                }
            }
        } else {
            for (var term : v.asStringArray()) {
                if (term.isEmpty()) {continue;}
                if (validTerms.stream().noneMatch(t -> t.equalsIgnoreCase(term))) {
                    LOG.warnf("@Disposition.%s value '%s' on %s may not be a valid term in vocabulary '%s'"
                              + " (build-time check uses enum constant names, not VocabularyTerm.value())",
                              field, term, classInfo.name(), vocabUri);
                }
            }
        }
    }


    private AnnotatedAgentConfig extractConfig(AnnotationInstance identity, ClassInfo classInfo,
                                                CombinedIndexBuildItem index,
                                                HashMap<String, String> derivedIds) {
        var config = new AnnotatedAgentConfig();

        config.agentId = resolveAgentId(identity, classInfo, derivedIds);
        config.name = resolveDisplayName(identity, classInfo);
        config.slot = stringValue(identity, "slot");
        config.provider = stringValue(identity, "provider");
        config.modelFamily = stringValue(identity, "modelFamily");
        config.jurisdiction = stringValue(identity, "jurisdiction");
        config.dataHandlingPolicy = stringValue(identity, "dataHandlingPolicy");
        config.briefing = stringValue(identity, "briefing");
        config.domainVocabulary = stringValue(identity, "vocabulary");
        config.slotVocabulary = stringValue(identity, "slotVocabulary");
        config.dispositionVocabulary = stringValue(identity, "dispositionVocabulary");
        config.styleVocabulary = stringValue(identity, "styleVocabulary");
        config.version = stringValue(identity, "version");
        config.weightsFingerprint = stringValue(identity, "weightsFingerprint");
        config.modelVersion = stringValue(identity, "modelVersion");

        extractDisposition(classInfo, config);
        extractGoals(classInfo, config);
        extractConstraints(classInfo, config);
        extractCapabilities(classInfo, config, index);
        extractTemplates(classInfo, config, index);

        validateGoalCapabilities(classInfo, config);

        return config;
    }

    private String resolveAgentId(AnnotationInstance identity, ClassInfo classInfo,
                                   HashMap<String, String> derivedIds) {
        var    explicit = stringValue(identity, "id");
        String agentId  = explicit.isEmpty() ? NameDerivation.toKebabCase(classInfo.simpleName()) : explicit;
        var    existing = derivedIds.put(agentId, classInfo.name().toString());
        if (existing != null) {
            throw new IllegalStateException(
                    "Duplicate agentId '" + agentId + "' from classes " + existing
                    + " and " + classInfo.name() + " — add explicit id() to at least one @Identity");
        }
        return agentId;
    }

    private String resolveDisplayName(AnnotationInstance identity, ClassInfo classInfo) {
        var explicit = stringValue(identity, "name");
        return explicit.isEmpty() ? NameDerivation.toDisplayName(classInfo.simpleName()) : explicit;
    }

    private void extractDisposition(ClassInfo classInfo, AnnotatedAgentConfig config) {
        var ann = classInfo.annotation(DISPOSITION);
        if (ann == null) {
            config.hasDisposition = false;
            return;
        }
        config.hasDisposition = true;
        config.socialOrient = stringValue(ann, "socialOrient");
        config.ruleFollowing = stringValue(ann, "ruleFollowing");
        config.riskAppetite = stringValue(ann, "riskAppetite");
        config.autonomy = stringValue(ann, "autonomy");
        config.conflictMode = stringValue(ann, "conflictMode");
        var del = ann.value("delegation");
        config.delegation = del != null && del.asBoolean();
        var dp = ann.value("dispositionProfile");
        if (dp != null) {
            var nested = dp.asNestedArray();
            config.dispositionProfile = new AnnotatedAgentConfig.DispositionWeightConfig[nested.length];
            for (int i = 0; i < nested.length; i++) {
                var dwc = new AnnotatedAgentConfig.DispositionWeightConfig();
                dwc.value = nested[i].value("value").asString();
                var w = nested[i].value("weight");
                dwc.weight = w != null ? w.asDouble() : 1.0;
                if (Double.isNaN(dwc.weight) || dwc.weight < 0.0 || dwc.weight > 1.0) {
                    throw new IllegalStateException(
                        "@DispositionWeight weight for '" + dwc.value + "' on " + classInfo.name()
                        + " must be 0.0-1.0, got " + dwc.weight);
                }
                config.dispositionProfile[i] = dwc;
            }
        }
        var sp = ann.value("styleProfile");
        if (sp != null) {
            var nested = sp.asNestedArray();
            config.styleProfile = new AnnotatedAgentConfig.DispositionWeightConfig[nested.length];
            for (int i = 0; i < nested.length; i++) {
                var dwc = new AnnotatedAgentConfig.DispositionWeightConfig();
                dwc.value = nested[i].value("value").asString();
                var w = nested[i].value("weight");
                dwc.weight = w != null ? w.asDouble() : 1.0;
                if (Double.isNaN(dwc.weight) || dwc.weight < 0.0 || dwc.weight > 1.0) {
                    throw new IllegalStateException(
                        "@DispositionWeight weight for '" + dwc.value + "' on " + classInfo.name()
                        + " must be 0.0-1.0, got " + dwc.weight);
                }
                config.styleProfile[i] = dwc;
            }
        }
        var av = ann.value("axisVocabularies");
        if (av != null) {
            var nested = av.asNestedArray();
            config.axisVocabularies = new AnnotatedAgentConfig.AxisVocabConfig[nested.length];
            for (int i = 0; i < nested.length; i++) {
                var avc = new AnnotatedAgentConfig.AxisVocabConfig();
                avc.axis = nested[i].value("axis").asEnum();
                avc.uri = nested[i].value("uri").asString();
                config.axisVocabularies[i] = avc;
            }
            var seenAxes = new HashSet<String>();
            for (var avc : config.axisVocabularies) {
                if (!seenAxes.add(avc.axis)) {
                    throw new IllegalStateException(
                        "Duplicate @AxisVocabulary axis " + avc.axis + " on " + classInfo.name());
                }
            }
        }
        config.mbtiType = stringValue(ann, "mbtiType");
        config.enneagramType = stringValue(ann, "enneagramType");
    }

    private void extractGoals(ClassInfo classInfo, AnnotatedAgentConfig config) {
        var ann = classInfo.annotation(AGENT_GOALS);
        if (ann == null) return;
        var defs = ann.value().asNestedArray();
        config.goals = new AnnotatedAgentConfig.GoalConfig[defs.length];
        for (int i = 0; i < defs.length; i++) {
            var g = new AnnotatedAgentConfig.GoalConfig();
            g.name = defs[i].value("name").asString();
            g.description = defs[i].value("description").asString();
            g.priority = enumValue(defs[i], "priority", "PRIMARY");
            g.visibility = enumValue(defs[i], "visibility", "PUBLIC");
            var caps = defs[i].value("capabilities");
            g.capabilities = caps != null ? caps.asStringArray() : new String[0];
            config.goals[i] = g;
        }
    }

    private void extractConstraints(ClassInfo classInfo, AnnotatedAgentConfig config) {
        var ann = classInfo.annotation(AGENT_CONSTRAINTS);
        if (ann == null) return;
        var defs = ann.value().asNestedArray();
        config.constraints = new AnnotatedAgentConfig.ConstraintConfig[defs.length];
        for (int i = 0; i < defs.length; i++) {
            var c = new AnnotatedAgentConfig.ConstraintConfig();
            c.name = defs[i].value("name").asString();
            c.description = defs[i].value("description").asString();
            c.severity = enumValue(defs[i], "severity", "HARD");
            c.visibility = enumValue(defs[i], "visibility", "PUBLIC");
            config.constraints[i] = c;
        }
    }

    private void extractCapabilities(ClassInfo classInfo, AnnotatedAgentConfig config, CombinedIndexBuildItem index) {
        var discAnn = classInfo.annotation(DISCOVERABLE);
        if (discAnn != null) {
            config.capabilities = discAnn.value("capabilities").asStringArray();
        }

        var capDefs = classInfo.annotationsWithRepeatable(AGENT_CAPABILITY_DEF, index.getIndex());
        if (!capDefs.isEmpty()) {
            config.richCapabilities = new AnnotatedAgentConfig.CapabilityConfig[capDefs.size()];
            var richNames = new HashSet<String>();
            for (int i = 0; i < capDefs.size(); i++) {
                var ann = capDefs.get(i);
                var cap = new AnnotatedAgentConfig.CapabilityConfig();
                cap.name = ann.value("name").asString();
                if (!richNames.add(cap.name)) {
                    throw new IllegalStateException(
                            "Duplicate @AgentCapabilityDef name '" + cap.name + "' on " + classInfo.name());
                }
                cap.description          = stringValue(ann, "description");
                cap.capabilityVocabulary = stringValue(ann, "capabilityVocabulary");
                var qh = ann.value("qualityHint");
                cap.qualityHint = qh != null ? qh.asDouble() : -1;
                var lh = ann.value("latencyHintP50Ms");
                cap.latencyHintP50Ms = lh != null ? lh.asLong() : -1;
                cap.costHint         = stringValue(ann, "costHint");
                var it = ann.value("inputTypes");
                cap.inputTypes = it != null ? it.asStringArray() : new String[0];
                var ot = ann.value("outputTypes");
                cap.outputTypes = ot != null ? ot.asStringArray() : new String[0];
                var tg = ann.value("tags");
                cap.tags = tg != null ? tg.asStringArray() : new String[0];
                var ed = ann.value("epistemicDomains");
                if (ed != null) {
                    var nested = ed.asNestedArray();
                    cap.epistemicDomains = new AnnotatedAgentConfig.EpistemicDomainConfig[nested.length];
                    for (int j = 0; j < nested.length; j++) {
                        var edc = new AnnotatedAgentConfig.EpistemicDomainConfig();
                        edc.value               = nested[j].value("value").asString();
                        edc.score               = nested[j].value("score").asDouble();
                        cap.epistemicDomains[j] = edc;
                    }
                }
                var exd = ann.value("excludedDomains");
                cap.excludedDomains        = exd != null ? exd.asStringArray() : new String[0];
                config.richCapabilities[i] = cap;
            }
        }

        validateCapabilityMetadata(classInfo, config.richCapabilities);

        if (config.capabilities != null && config.richCapabilities != null) {
            var discNames = new HashSet<>(java.util.Arrays.asList(config.capabilities));
            for (var rc : config.richCapabilities) {
                if (discNames.contains(rc.name)) {
                    throw new IllegalStateException(
                            "Capability '" + rc.name + "' on " + classInfo.name()
                            + " appears in both @Discoverable and @AgentCapabilityDef");
                }
            }
        }
    }

    private void extractTemplates(ClassInfo classInfo, AnnotatedAgentConfig config, CombinedIndexBuildItem index) {
        var templateDefs = classInfo.annotationsWithRepeatable(AGENT_TEMPLATE_REF, index.getIndex());
        if (templateDefs.isEmpty()) {return;}
        config.templateRefs = new AnnotatedAgentConfig.TemplateRefConfig[templateDefs.size()];
        for (int i = 0; i < templateDefs.size(); i++) {
            var ann = templateDefs.get(i);
            var ref = new AnnotatedAgentConfig.TemplateRefConfig();
            ref.id = ann.value("id").asString();
            var argsVal = ann.value("args");
            if (argsVal != null) {
                var nested = argsVal.asNestedArray();
                ref.args = new AnnotatedAgentConfig.TemplateArgConfig[nested.length];
                for (int j = 0; j < nested.length; j++) {
                    var tac = new AnnotatedAgentConfig.TemplateArgConfig();
                    tac.key     = nested[j].value("key").asString();
                    tac.value   = nested[j].value("value").asString();
                    ref.args[j] = tac;
                }
            }
            config.templateRefs[i] = ref;
        }
    }


    private void validateGoalCapabilities(ClassInfo classInfo, AnnotatedAgentConfig config) {
        if (config.goals == null) {return;}
        var capNames = new HashSet<String>();
        if (config.capabilities != null) {
            for (var cap : config.capabilities) {capNames.add(cap);}
        }
        if (config.richCapabilities != null) {
            for (var cap : config.richCapabilities) {capNames.add(cap.name);}
        }
        if (capNames.isEmpty()) {return;}
        for (var goal : config.goals) {
            if (goal.capabilities == null) {continue;}
            for (var capRef : goal.capabilities) {
                if (!capNames.contains(capRef)) {
                    throw new IllegalStateException(
                            "@AgentGoalDef '" + goal.name + "' on " + classInfo.name()
                            + " references capability '" + capRef + "' not declared in @Discoverable or @AgentCapabilityDef");
                }
            }
        }
    }

    private void validateCapabilityMetadata(ClassInfo classInfo, AnnotatedAgentConfig.CapabilityConfig[] caps) {
        if (caps == null) {return;}
        for (var cap : caps) {
            if (cap.qualityHint != -1) {
                if (Double.isNaN(cap.qualityHint) || cap.qualityHint < 0.0 || cap.qualityHint > 1.0) {
                    throw new IllegalStateException(
                            "@AgentCapabilityDef '" + cap.name + "' on " + classInfo.name()
                            + ": qualityHint must be 0.0-1.0, got " + cap.qualityHint);
                }
            }
            if (cap.epistemicDomains != null) {
                var domainNames = new HashSet<String>();
                for (var ed : cap.epistemicDomains) {
                    if (Double.isNaN(ed.score) || ed.score < 0.0 || ed.score > 1.0) {
                        throw new IllegalStateException(
                                "@EpistemicDomain score for '" + ed.value + "' on " + classInfo.name()
                                + " must be 0.0-1.0, got " + ed.score);
                    }
                    domainNames.add(ed.value);
                }
                if (cap.excludedDomains != null) {
                    for (var exd : cap.excludedDomains) {
                        if (domainNames.contains(exd)) {
                            throw new IllegalStateException(
                                    "Domain '" + exd + "' on " + classInfo.name()
                                    + " appears in both epistemicDomains and excludedDomains");
                        }
                    }
                }
            }
        }
    }


    private static String stringValue(AnnotationInstance ann, String key) {
        return AnnotationProcessorUtils.stringValue(ann, key);
    }

    private static String enumValue(AnnotationInstance ann, String key, String defaultValue) {
        return AnnotationProcessorUtils.enumValue(ann, key, defaultValue);
    }
}

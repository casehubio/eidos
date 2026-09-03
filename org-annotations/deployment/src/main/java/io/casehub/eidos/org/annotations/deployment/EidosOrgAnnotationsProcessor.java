package io.casehub.eidos.org.annotations.deployment;

import io.casehub.eidos.annotations.AgentCapabilityDef;
import io.casehub.eidos.annotations.AgentConstraints;
import io.casehub.eidos.annotations.AgentGoals;
import io.casehub.eidos.annotations.NameDerivation;
import io.casehub.eidos.annotations.deployment.AnnotationProcessorUtils;
import io.casehub.eidos.annotations.deployment.EidosAnnotationProcessedBuildItem;
import io.casehub.eidos.annotations.runtime.AnnotatedAgentConfig;
import io.casehub.eidos.org.annotations.AttestationGrantDef;
import io.casehub.eidos.org.annotations.OrgMembers;
import io.casehub.eidos.org.annotations.OrgRelationships;
import io.casehub.eidos.org.annotations.OrgUnit;
import io.casehub.eidos.org.annotations.Supervises;
import io.casehub.eidos.org.annotations.Supervisions;
import io.casehub.eidos.org.annotations.runtime.AnnotatedOrgConfig;
import io.casehub.eidos.org.annotations.runtime.EidosOrgAnnotationsRecorder;
import io.casehub.eidos.org.api.spi.OrgRegistrar;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

class EidosOrgAnnotationsProcessor {
    private static final org.jboss.logging.Logger LOG = org.jboss.logging.Logger.getLogger(EidosOrgAnnotationsProcessor.class);

    private static final String FEATURE = "eidos-org-annotations";
    private static final DotName ORG_UNIT = DotName.createSimple(OrgUnit.class);
    private static final DotName ORG_MEMBERS = DotName.createSimple(OrgMembers.class);
    private static final DotName SUPERVISES = DotName.createSimple(Supervises.class);
    private static final DotName SUPERVISIONS = DotName.createSimple(Supervisions.class);
    private static final DotName ORG_RELATIONSHIPS = DotName.createSimple(OrgRelationships.class);

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    void processAnnotations(
            EidosOrgAnnotationsRecorder recorder,
            CombinedIndexBuildItem index,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            Optional<EidosAnnotationProcessedBuildItem> eidosProcessed) {

        var unitAnnotations = index.getIndex().getAnnotations(ORG_UNIT);
        if (unitAnnotations.isEmpty()) {return;}

        var processedClasses = new java.util.HashSet<String>();
        var derivedIds       = new HashMap<String, String>();

        for (var annotation : unitAnnotations) {
            if (annotation.target().kind() != AnnotationTarget.Kind.CLASS) {continue;}
            var classInfo = annotation.target().asClass();
            var className = classInfo.name().toString();
            processedClasses.add(className);

            var config = extractConfig(annotation, classInfo, derivedIds);

            syntheticBeans.produce(SyntheticBeanBuildItem
                                           .configure(OrgRegistrar.class)
                                           .scope(ApplicationScoped.class)
                                           .identifier("eidos-org-ann-" + className)
                                           .setRuntimeInit()
                                           .createWith(recorder.createRegistrar(config))
                                           .done());
        }

        warnOrphanAnnotations(index, processedClasses);
        crossValidateMembers(index, processedClasses, eidosProcessed);
    }

    private AnnotatedOrgConfig extractConfig(AnnotationInstance orgUnit, ClassInfo classInfo,
                                              HashMap<String, String> derivedIds) {
        var config = new AnnotatedOrgConfig();

        config.unitId = resolveUnitId(orgUnit, classInfo, derivedIds);
        config.name = resolveDisplayName(orgUnit, classInfo);
        config.kind = stringValue(orgUnit, "kind");
        config.kindVocabulary = stringValue(orgUnit, "kindVocabulary");
        config.parentUnit = stringValue(orgUnit, "parentUnit");

        extractMembers(classInfo, config);
        extractRelationships(classInfo, config);
        extractCapabilities(orgUnit, config);
        extractGoals(orgUnit, config);
        extractConstraints(orgUnit, config);

        return config;
    }

    private String resolveUnitId(AnnotationInstance orgUnit, ClassInfo classInfo,
                                  HashMap<String, String> derivedIds) {
        var explicit = stringValue(orgUnit, "id");
        String unitId = explicit.isEmpty() ? NameDerivation.toKebabCase(classInfo.simpleName()) : explicit;
        var existing = derivedIds.put(unitId, classInfo.name().toString());
        if (existing != null) {
            throw new IllegalStateException(
                    "Duplicate unitId '" + unitId + "' from classes " + existing
                    + " and " + classInfo.name() + " — add explicit id() to at least one @OrgUnit");
        }
        return unitId;
    }

    private String resolveDisplayName(AnnotationInstance orgUnit, ClassInfo classInfo) {
        var explicit = stringValue(orgUnit, "name");
        return explicit.isEmpty() ? NameDerivation.toDisplayName(classInfo.simpleName()) : explicit;
    }

    private void extractMembers(ClassInfo classInfo, AnnotatedOrgConfig config) {
        var ann = classInfo.annotation(ORG_MEMBERS);
        if (ann == null) return;
        var defs = ann.value().asNestedArray();
        config.members = new AnnotatedOrgConfig.MemberConfig[defs.length];
        for (int i = 0; i < defs.length; i++) {
            var m = new AnnotatedOrgConfig.MemberConfig();
            m.agentId = defs[i].value("agentId").asString();
            m.role = stringValue(defs[i], "role");
            m.roleVocabulary = stringValue(defs[i], "roleVocabulary");
            config.members[i] = m;
        }
    }

    private void extractRelationships(ClassInfo classInfo, AnnotatedOrgConfig config) {
        var rels = new ArrayList<AnnotatedOrgConfig.RelationshipConfig>();

        // @Supervises (single)
        var singleSupervises = classInfo.annotation(SUPERVISES);
        if (singleSupervises != null && classInfo.annotation(SUPERVISIONS) == null) {
            rels.add(toSupervisionConfig(singleSupervises));
        }

        // @Supervisions (repeatable container)
        var supervisesContainer = classInfo.annotation(SUPERVISIONS);
        if (supervisesContainer != null) {
            for (var s : supervisesContainer.value().asNestedArray()) {
                rels.add(toSupervisionConfig(s));
            }
        }

        // @OrgRelationships
        var orgRels = classInfo.annotation(ORG_RELATIONSHIPS);
        if (orgRels != null) {
            for (var r : orgRels.value().asNestedArray()) {
                var rc = new AnnotatedOrgConfig.RelationshipConfig();
                rc.source = r.value("source").asString();
                rc.target = r.value("target").asString();
                rc.kind = r.value("kind").asString();
                rc.extendedKind = stringValue(r, "extendedKind");
                rc.kindVocabulary = stringValue(r, "kindVocabulary");
                rc.scope = stringValue(r, "scope");
                rc.scopeDomain = stringValue(r, "scopeDomain");
                rc.scopeCondition = stringValue(r, "scopeCondition");
                rc.attestation = extractAttestation(r);
                rels.add(rc);
            }
        }

        if (!rels.isEmpty()) {
            config.relationships = rels.toArray(new AnnotatedOrgConfig.RelationshipConfig[0]);
        }
    }

    private AnnotatedOrgConfig.RelationshipConfig toSupervisionConfig(AnnotationInstance ann) {
        var rc = new AnnotatedOrgConfig.RelationshipConfig();
        rc.source = ann.value("source").asString();
        rc.target = ann.value("target").asString();
        rc.kind = "SUPERVISES";
        rc.scope = stringValue(ann, "scope");
        rc.scopeDomain = stringValue(ann, "scopeDomain");
        rc.scopeCondition = stringValue(ann, "scopeCondition");
        return rc;
    }

    private void extractCapabilities(AnnotationInstance orgUnit, AnnotatedOrgConfig config) {
        var v = orgUnit.value("capabilities");
        if (v == null) return;
        var defs = v.asNestedArray();
        if (defs.length == 0) return;
        config.capabilities = new AnnotatedAgentConfig.CapabilityConfig[defs.length];
        var names = new java.util.HashSet<String>();
        for (int i = 0; i < defs.length; i++) {
            var cap = new AnnotatedAgentConfig.CapabilityConfig();
            cap.name = defs[i].value("name").asString();
            if (!names.add(cap.name)) {
                throw new IllegalStateException("Duplicate capability name '" + cap.name + "' in @OrgUnit");
            }
            cap.description = stringValue(defs[i], "description");
            cap.capabilityVocabulary = stringValue(defs[i], "capabilityVocabulary");
            var qh = defs[i].value("qualityHint");
            cap.qualityHint = qh != null ? qh.asDouble() : -1;
            var lh = defs[i].value("latencyHintP50Ms");
            cap.latencyHintP50Ms = lh != null ? lh.asLong() : -1;
            cap.costHint = stringValue(defs[i], "costHint");
            var it = defs[i].value("inputTypes");
            cap.inputTypes = it != null ? it.asStringArray() : new String[0];
            var ot = defs[i].value("outputTypes");
            cap.outputTypes = ot != null ? ot.asStringArray() : new String[0];
            var tg = defs[i].value("tags");
            cap.tags = tg != null ? tg.asStringArray() : new String[0];
            var ed = defs[i].value("epistemicDomains");
            if (ed != null) {
                var nested = ed.asNestedArray();
                cap.epistemicDomains = new AnnotatedAgentConfig.EpistemicDomainConfig[nested.length];
                for (int j = 0; j < nested.length; j++) {
                    var edc = new AnnotatedAgentConfig.EpistemicDomainConfig();
                    edc.value = nested[j].value("value").asString();
                    edc.score = nested[j].value("score").asDouble();
                    cap.epistemicDomains[j] = edc;
                }
            }
            var exd = defs[i].value("excludedDomains");
            cap.excludedDomains = exd != null ? exd.asStringArray() : new String[0];
            config.capabilities[i] = cap;
        }
    }

    private void extractGoals(AnnotationInstance orgUnit, AnnotatedOrgConfig config) {
        var v = orgUnit.value("goals");
        if (v == null) return;
        var defs = v.asNestedArray();
        if (defs.length == 0) return;
        config.goals = new AnnotatedAgentConfig.GoalConfig[defs.length];
        for (int i = 0; i < defs.length; i++) {
            var g = new AnnotatedAgentConfig.GoalConfig();
            g.name = defs[i].value("name").asString();
            g.description = defs[i].value("description").asString();
            g.priority = enumValue(defs[i], "priority", "PRIMARY");
            g.visibility = enumValue(defs[i], "visibility", "PUBLIC");
            var caps = defs[i].value("capabilities");
            g.capabilities = caps != null ? caps.asStringArray() : new String[0];
            var attrs = defs[i].value("attributes");
            if (attrs != null) {
                var nested = attrs.asNestedArray();
                g.attributes = new AnnotatedAgentConfig.TemplateArgConfig[nested.length];
                for (int j = 0; j < nested.length; j++) {
                    var ac = new AnnotatedAgentConfig.TemplateArgConfig();
                    ac.key = nested[j].value("key").asString();
                    ac.value = nested[j].value("value").asString();
                    g.attributes[j] = ac;
                }
            }
            config.goals[i] = g;
        }
    }

    private void extractConstraints(AnnotationInstance orgUnit, AnnotatedOrgConfig config) {
        var v = orgUnit.value("constraints");
        if (v == null) return;
        var defs = v.asNestedArray();
        if (defs.length == 0) return;
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

    private AnnotatedOrgConfig.AttestationConfig extractAttestation(AnnotationInstance ann) {
        var v = ann.value("attestation");
        if (v == null) return null;
        var defs = v.asNestedArray();
        if (defs.length == 0) return null;
        var att = defs[0];
        var dims = att.value("dimensions");
        if (dims == null || dims.asStringArray().length == 0) return null;
        var ac = new AnnotatedOrgConfig.AttestationConfig();
        ac.dimensions = dims.asStringArray();
        var cs = att.value("capabilityScope");
        ac.capabilityScope = cs != null ? cs.asStringArray() : new String[0];
        var st = att.value("signalTypes");
        ac.signalTypes = st != null ? st.asStringArray() : new String[0];
        return ac;
    }

    private void warnOrphanAnnotations(CombinedIndexBuildItem index, java.util.Set<String> processedClasses) {
        for (var dotName : java.util.List.of(ORG_MEMBERS)) {
            for (var ann : index.getIndex().getAnnotations(dotName)) {
                if (ann.target().kind() != AnnotationTarget.Kind.CLASS) continue;
                var className = ann.target().asClass().name().toString();
                if (!processedClasses.contains(className)) {
                    LOG.warnf("Class %s has @OrgMembers but no @OrgUnit — members will not be registered", className);
                }
            }
        }
        for (var dotName : java.util.List.of(SUPERVISES, SUPERVISIONS)) {
            for (var ann : index.getIndex().getAnnotations(dotName)) {
                if (ann.target().kind() != AnnotationTarget.Kind.CLASS) continue;
                var className = ann.target().asClass().name().toString();
                if (!processedClasses.contains(className)) {
                    LOG.warnf("Class %s has @Supervises but no @OrgUnit — relationships will not be registered", className);
                }
            }
        }
        for (var ann : index.getIndex().getAnnotations(ORG_RELATIONSHIPS)) {
            if (ann.target().kind() != AnnotationTarget.Kind.CLASS) continue;
            var className = ann.target().asClass().name().toString();
            if (!processedClasses.contains(className)) {
                LOG.warnf("Class %s has @OrgRelationships but no @OrgUnit — relationships will not be registered", className);
            }
        }
    }

    private void crossValidateMembers(CombinedIndexBuildItem index,
                                       java.util.Set<String> processedClasses,
                                       Optional<EidosAnnotationProcessedBuildItem> eidosProcessed) {
        if (eidosProcessed.isEmpty()) return;
        var identityClasses = eidosProcessed.get().processedClassNames();
        for (var ann : index.getIndex().getAnnotations(ORG_MEMBERS)) {
            if (ann.target().kind() != AnnotationTarget.Kind.CLASS) continue;
            var defs = ann.value().asNestedArray();
            for (var def : defs) {
                var agentId = def.value("agentId").asString();
                if (agentId != null && !agentId.isEmpty()) {
                    LOG.debugf("@OrgMemberDef agentId '%s' — cross-validation with @Identity agents available", agentId);
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

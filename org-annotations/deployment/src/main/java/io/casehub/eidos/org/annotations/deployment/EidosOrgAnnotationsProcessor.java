package io.casehub.eidos.org.annotations.deployment;

import io.casehub.eidos.annotations.NameDerivation;
import io.casehub.eidos.annotations.deployment.AnnotationProcessorUtils;
import io.casehub.eidos.annotations.deployment.EidosAnnotationProcessedBuildItem;
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
        return rc;
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
}

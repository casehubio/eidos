package io.casehub.eidos.org.annotations.deployment;

import io.casehub.eidos.org.annotations.NameDerivation;
import io.casehub.eidos.org.annotations.OrgMembers;
import io.casehub.eidos.org.annotations.OrgRelationships;
import io.casehub.eidos.org.annotations.OrgUnit;
import io.casehub.eidos.org.annotations.Supervises;
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

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

class EidosOrgAnnotationsProcessor {

    private static final String FEATURE = "eidos-org-annotations";
    private static final DotName ORG_UNIT = DotName.createSimple(OrgUnit.class);
    private static final DotName ORG_MEMBERS = DotName.createSimple(OrgMembers.class);
    private static final DotName SUPERVISES = DotName.createSimple(Supervises.class);
    private static final DotName SUPERVISES_LIST = DotName.createSimple(Supervises.List.class);
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
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {

        var unitAnnotations = index.getIndex().getAnnotations(ORG_UNIT);
        if (unitAnnotations.isEmpty()) return;

        var derivedIds = new HashMap<String, String>();

        for (var annotation : unitAnnotations) {
            if (annotation.target().kind() != AnnotationTarget.Kind.CLASS) continue;
            var classInfo = annotation.target().asClass();
            var className = classInfo.name().toString();

            var config = extractConfig(annotation, classInfo, derivedIds);

            syntheticBeans.produce(SyntheticBeanBuildItem
                    .configure(OrgRegistrar.class)
                    .scope(ApplicationScoped.class)
                    .identifier("eidos-org-ann-" + className)
                    .setRuntimeInit()
                    .supplier(recorder.createRegistrar(config))
                    .done());
        }
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
        if (singleSupervises != null && classInfo.annotation(SUPERVISES_LIST) == null) {
            rels.add(toSupervisionConfig(singleSupervises));
        }

        // @Supervises.List (repeatable container)
        var supervisesContainer = classInfo.annotation(SUPERVISES_LIST);
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

    private static String stringValue(AnnotationInstance ann, String key) {
        var v = ann.value(key);
        return v != null ? v.asString() : "";
    }
}

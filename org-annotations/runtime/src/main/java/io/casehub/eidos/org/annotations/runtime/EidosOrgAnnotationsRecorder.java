package io.casehub.eidos.org.annotations.runtime;

import io.casehub.eidos.annotations.runtime.AnnotatedAgentConfig;
import io.casehub.eidos.api.AgentCapability;
import io.casehub.eidos.api.AgentConstraint;
import io.casehub.eidos.api.AgentGoal;
import io.casehub.eidos.api.BehavioralSignal;
import io.casehub.eidos.api.ConstraintSeverity;
import io.casehub.eidos.api.GoalPriority;
import io.casehub.eidos.api.Visibility;
import io.casehub.eidos.org.api.AgentRelationship;
import io.casehub.eidos.org.api.AttestationGrant;
import io.casehub.eidos.org.api.Membership;
import io.casehub.eidos.org.api.OrganizationalUnit;
import io.casehub.eidos.org.api.RelationshipKind;
import io.casehub.eidos.org.api.RelationshipScope;
import io.casehub.eidos.org.api.spi.OrgRegistrar;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.runtime.annotations.Recorder;
import org.eclipse.microprofile.config.ConfigProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Recorder
public class EidosOrgAnnotationsRecorder {

    private static final String TENANCY_CONFIG_KEY = "casehub.eidos.annotations.default-tenancy-id";

    public Function<SyntheticCreationalContext<OrgRegistrar>, OrgRegistrar>
    createRegistrar(AnnotatedOrgConfig config) {
        return ctx -> {
            var tenancyId = ConfigProvider.getConfig()
                                          .getOptionalValue(TENANCY_CONFIG_KEY, String.class)
                                          .orElse("default");

            var builder = OrganizationalUnit.builder()
                                            .unitId(config.unitId).name(config.name).tenancyId(tenancyId);

            if (notEmpty(config.kind)) {builder.kind(config.kind);}
            if (notEmpty(config.kindVocabulary)) {builder.kindVocabulary(config.kindVocabulary);}
            if (notEmpty(config.parentUnit)) {builder.parentUnitId(config.parentUnit);}

            if (config.members != null && config.members.length > 0) {
                var members = new ArrayList<Membership>();
                for (var m : config.members) {
                    members.add(new Membership(
                            m.agentId,
                            notEmpty(m.role) ? m.role : null,
                            notEmpty(m.roleVocabulary) ? m.roleVocabulary : null));
                }
                builder.members(members);
            }

            if (config.capabilities != null && config.capabilities.length > 0) {
                builder.capabilities(buildCapabilities(config.capabilities));
            }
            if (config.goals != null && config.goals.length > 0) {
                builder.goals(buildGoals(config.goals));
            }
            if (config.constraints != null && config.constraints.length > 0) {
                builder.constraints(buildConstraints(config.constraints));
            }

            var unit          = builder.build();
            var relationships = new ArrayList<AgentRelationship>();

            if (config.relationships != null) {
                for (var r : config.relationships) {
                    var relBuilder = AgentRelationship.builder()
                                                      .sourceAgentId(r.source).targetAgentId(r.target)
                                                      .kind(RelationshipKind.valueOf(r.kind)).tenancyId(tenancyId);

                    if (notEmpty(r.extendedKind)) {relBuilder.extendedKind(r.extendedKind);}
                    if (notEmpty(r.kindVocabulary)) {relBuilder.kindVocabulary(r.kindVocabulary);}
                    relBuilder.scope(buildScope(r));
                    if (r.attestation != null) {relBuilder.attestation(buildAttestation(r.attestation));}

                    relationships.add(relBuilder.build());
                }
            }

            var def = new OrgRegistrar.OrgDefinition(List.of(unit), relationships);
            return (OrgRegistrar) () -> def;
        };
    }

    private static List<AgentCapability> buildCapabilities(AnnotatedAgentConfig.CapabilityConfig[] caps) {
        var result = new ArrayList<AgentCapability>();
        for (var cap : caps) {
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
            result.add(cb.build());
        }
        return result;
    }

    private static List<AgentGoal> buildGoals(AnnotatedAgentConfig.GoalConfig[] goals) {
        var result = new ArrayList<AgentGoal>();
        for (var g : goals) {
            Map<String, String> attrs = null;
            if (g.attributes != null && g.attributes.length > 0) {
                attrs = new HashMap<>();
                for (var a : g.attributes) {attrs.put(a.key, a.value);}
            }
            result.add(new AgentGoal(g.name, g.description,
                    GoalPriority.valueOf(g.priority), Visibility.valueOf(g.visibility),
                    g.capabilities != null ? List.of(g.capabilities) : List.of(), attrs));
        }
        return result;
    }

    private static List<AgentConstraint> buildConstraints(AnnotatedAgentConfig.ConstraintConfig[] constraints) {
        var result = new ArrayList<AgentConstraint>();
        for (var c : constraints) {
            result.add(new AgentConstraint(c.name, c.description,
                    Visibility.valueOf(c.visibility), ConstraintSeverity.valueOf(c.severity)));
        }
        return result;
    }

    private static RelationshipScope buildScope(AnnotatedOrgConfig.RelationshipConfig r) {
        var cap = notEmpty(r.scope) ? r.scope : null;
        var domain = notEmpty(r.scopeDomain) ? r.scopeDomain : null;
        var condition = notEmpty(r.scopeCondition) ? r.scopeCondition : null;
        if (cap == null && domain == null && condition == null) return null;
        return new RelationshipScope(cap, domain, condition);
    }

    private static AttestationGrant buildAttestation(AnnotatedOrgConfig.AttestationConfig att) {
        var dims = Set.of(att.dimensions);
        var capScope = att.capabilityScope != null && att.capabilityScope.length > 0
                ? Set.of(att.capabilityScope) : Set.<String>of();
        var signals = new HashSet<BehavioralSignal>();
        if (att.signalTypes != null) {
            for (var s : att.signalTypes) {signals.add(BehavioralSignal.valueOf(s));}
        }
        return new AttestationGrant(dims, capScope, signals.isEmpty() ? Set.of() : signals);
    }

    private static boolean notEmpty(String s) {
        return s != null && !s.isEmpty();
    }
}

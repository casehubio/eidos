package io.casehub.eidos.org.annotations.runtime;

import io.casehub.eidos.org.api.AgentRelationship;
import io.casehub.eidos.org.api.Membership;
import io.casehub.eidos.org.api.OrganizationalUnit;
import io.casehub.eidos.org.api.RelationshipKind;
import io.casehub.eidos.org.api.RelationshipScope;
import io.casehub.eidos.org.api.spi.OrgRegistrar;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.runtime.annotations.Recorder;
import org.eclipse.microprofile.config.ConfigProvider;

import java.util.ArrayList;
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

            var unit          = builder.build();
            var relationships = new ArrayList<AgentRelationship>();

            if (config.relationships != null) {
                for (var r : config.relationships) {
                    var relBuilder = AgentRelationship.builder()
                                                      .sourceAgentId(r.source).targetAgentId(r.target)
                                                      .kind(RelationshipKind.valueOf(r.kind)).tenancyId(tenancyId);

                    if (notEmpty(r.extendedKind)) {relBuilder.extendedKind(r.extendedKind);}
                    if (notEmpty(r.kindVocabulary)) {relBuilder.kindVocabulary(r.kindVocabulary);}
                    if (notEmpty(r.scope)) {relBuilder.scope(new RelationshipScope(r.scope, null, null));}

                    relationships.add(relBuilder.build());
                }
            }

            var def = new OrgRegistrar.OrgDefinition(java.util.List.of(unit), relationships);
            return (OrgRegistrar) () -> def;
        };
    }

    private static boolean notEmpty(String s) {
        return s != null && !s.isEmpty();
    }
}

package io.casehub.eidos.annotations.runtime;

import io.casehub.eidos.api.AgentCapability;
import io.casehub.eidos.api.AgentConstraint;
import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentDisposition;
import io.casehub.eidos.api.AgentGoal;
import io.casehub.eidos.api.ConstraintSeverity;
import io.casehub.eidos.api.DispositionValue;
import io.casehub.eidos.api.GoalPriority;
import io.casehub.eidos.api.Visibility;
import io.casehub.eidos.api.spi.AgentDescriptorRegistrar;
import io.quarkus.runtime.annotations.Recorder;
import org.eclipse.microprofile.config.ConfigProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Recorder
public class EidosAnnotationsRecorder {

    private static final String TENANCY_CONFIG_KEY = "casehub.eidos.annotations.default-tenancy-id";

    public Supplier<AgentDescriptorRegistrar> createRegistrar(AnnotatedAgentConfig config) {
        return () -> {
            var tenancyId = ConfigProvider.getConfig()
                    .getOptionalValue(TENANCY_CONFIG_KEY, String.class)
                    .orElse("default");

            var builder = AgentDescriptor.builder()
                    .agentId(config.agentId).name(config.name).slot(config.slot).tenancyId(tenancyId);

            if (notEmpty(config.provider)) builder.provider(config.provider);
            if (notEmpty(config.modelFamily)) builder.modelFamily(config.modelFamily);
            if (notEmpty(config.jurisdiction)) builder.jurisdiction(config.jurisdiction);
            if (notEmpty(config.dataHandlingPolicy)) builder.dataHandlingPolicy(config.dataHandlingPolicy);
            if (notEmpty(config.briefing)) builder.briefing(config.briefing);
            if (notEmpty(config.domainVocabulary)) builder.domainVocabulary(config.domainVocabulary);
            if (notEmpty(config.slotVocabulary)) builder.slotVocabulary(config.slotVocabulary);
            if (notEmpty(config.dispositionVocabulary)) builder.dispositionVocabulary(config.dispositionVocabulary);
            if (notEmpty(config.styleVocabulary)) builder.styleVocabulary(config.styleVocabulary);
            if (notEmpty(config.version)) builder.version(config.version);

            if (config.hasDisposition) {
                var db = AgentDisposition.builder().delegation(config.delegation);
                if (notEmpty(config.socialOrient)) db.socialOrient(config.socialOrient);
                if (notEmpty(config.ruleFollowing)) db.ruleFollowing(config.ruleFollowing);
                if (notEmpty(config.riskAppetite)) db.riskAppetite(config.riskAppetite);
                if (notEmpty(config.autonomy)) db.autonomy(config.autonomy);
                if (notEmpty(config.conflictMode)) db.conflictMode(config.conflictMode);
                if (config.dispositionProfile != null && config.dispositionProfile.length > 0) {
                    var values = new ArrayList<DispositionValue>();
                    for (var t : config.dispositionProfile) if (notEmpty(t)) values.add(DispositionValue.of(t));
                    if (!values.isEmpty()) db.dispositionProfile(values);
                }
                if (config.styleProfile != null && config.styleProfile.length > 0) {
                    var values = new ArrayList<DispositionValue>();
                    for (var t : config.styleProfile) if (notEmpty(t)) values.add(DispositionValue.of(t));
                    if (!values.isEmpty()) db.styleProfile(values);
                }
                builder.disposition(db.build());
            }

            if (config.goals != null) {
                var goals = new ArrayList<AgentGoal>();
                for (var g : config.goals) {
                    goals.add(new AgentGoal(g.name, g.description,
                            GoalPriority.valueOf(g.priority), Visibility.valueOf(g.visibility),
                            g.capabilities != null ? List.of(g.capabilities) : List.of()));
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

            if (config.capabilities != null && config.capabilities.length > 0) {
                var caps = new ArrayList<AgentCapability>();
                for (var name : config.capabilities) {
                    caps.add(new AgentCapability.Builder().name(name).build());
                }
                builder.capabilities(caps);
            }

            return (AgentDescriptorRegistrar) () -> List.of(builder.build());
        };
    }

    private static boolean notEmpty(String s) {
        return s != null && !s.isEmpty();
    }
}

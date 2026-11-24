package io.casehub.eidos.runtime.health;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentGraphQuery;
import io.casehub.eidos.api.CapabilityHealth.ProbeContext;
import io.casehub.eidos.api.GraphDataSufficiency;
import io.casehub.eidos.api.RoleHealth;
import io.casehub.eidos.api.SufficiencyLevel;
import io.casehub.eidos.api.TaskResult;
import io.casehub.eidos.api.WilsonConfidence;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@ApplicationScoped
public class DefaultRoleHealth implements RoleHealth {

    private final AgentGraphQuery graphQuery;
    private final int windowDays;

    @Inject
    public DefaultRoleHealth(final AgentGraphQuery graphQuery,
                             @ConfigProperty(name = "casehub.eidos.role-health.window-days",
                                     defaultValue = "90") final int windowDays) {
        this.graphQuery = graphQuery;
        this.windowDays = windowDays;
    }

    @Override
    public RoleStatus probe(final AgentDescriptor descriptor, final String roleName,
                            final ProbeContext context) {
        if (descriptor.roleProfile() == null || !descriptor.roleProfile().hasRole(roleName)) {
            return new RoleStatus.Undeclared(roleName);
        }

        final Instant since = Instant.now().minus(windowDays, ChronoUnit.DAYS);
        final var history = graphQuery.historyByRole(
                descriptor.agentId(), roleName, descriptor.tenancyId(), since);

        final int n = history.outcomes().size();
        if (GraphDataSufficiency.forCount(n, since, Instant.now(), List.of()).level()
                == SufficiencyLevel.INSUFFICIENT) {
            return new RoleStatus.DeclaredFit(roleName);
        }

        double sumQuality = 0.0;
        int failureCount = 0;
        for (final var outcome : history.outcomes()) {
            sumQuality += outcome.confidence() * multiplier(outcome.result());
            if (outcome.result() == TaskResult.FAILED) failureCount++;
        }

        final double confidence = WilsonConfidence.score(sumQuality, n);
        final var policy = descriptor.roleProfile().changePolicy();

        if (confidence >= policy.minConfidence()) {
            return new RoleStatus.Fit(roleName, confidence);
        } else if (confidence >= policy.demotionConfidence()) {
            return new RoleStatus.Marginal(roleName, confidence, n);
        } else {
            return new RoleStatus.Unfit(roleName, confidence, failureCount, n);
        }
    }

    private static double multiplier(final TaskResult result) {
        return switch (result) {
            case SUCCEEDED -> 1.0;
            case PARTIALLY -> 0.5;
            case FAILED -> 0.0;
        };
    }
}

package io.casehub.eidos.runtime.health;

import io.casehub.eidos.api.AgentGraphQuery;
import io.casehub.eidos.api.CapabilityHealth.ProbeContext;
import io.casehub.eidos.api.RoleEvidence;
import io.casehub.eidos.api.RoleEvidenceSource;
import io.casehub.eidos.api.RoleOutcome;
import io.casehub.eidos.api.TaskResult;
import io.casehub.eidos.api.TemporalDecay;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class TaskOutcomeEvidenceSource implements RoleEvidenceSource {

    private final AgentGraphQuery graphQuery;
    private final int windowDays;
    private final double halfLifeDays;

    @Inject
    public TaskOutcomeEvidenceSource(
            final AgentGraphQuery graphQuery,
            @ConfigProperty(name = "casehub.eidos.role-health.window-days",
                    defaultValue = "90") final int windowDays,
            @ConfigProperty(name = "casehub.eidos.role-health.decay-half-life-days",
                    defaultValue = "90") final double halfLifeDays) {
        this.graphQuery = graphQuery;
        this.windowDays = windowDays;
        this.halfLifeDays = halfLifeDays;
    }

    @Override
    public String sourceId() {
        return "task-outcome";
    }

    @Override
    public RoleEvidence evaluate(final String agentId, final String roleName,
                                  final String tenancyId, final ProbeContext context) {
        final Instant since = Instant.now().minus(windowDays, ChronoUnit.DAYS);
        final var history = context.taskDomain() != null
                ? graphQuery.historyByRole(agentId, roleName, tenancyId, since, context.taskDomain())
                : graphQuery.historyByRole(agentId, roleName, tenancyId, since);

        final Map<String, RoleOutcome> roleOutcomesByTask = history.roleOutcomes().stream()
                .collect(Collectors.toMap(RoleOutcome::taskId, Function.identity()));

        final Instant now = Instant.now();
        double qualitySum = 0.0;
        double effectiveCount = 0.0;

        for (final var outcome : history.outcomes()) {
            final double decay = TemporalDecay.exponential(outcome.observedAt(), now, halfLifeDays);
            final RoleOutcome roleOutcome = roleOutcomesByTask.get(outcome.taskId());
            final double quality;
            if (roleOutcome != null) {
                quality = roleOutcome.roleQuality();
            } else {
                quality = outcome.confidence() * multiplier(outcome.result());
            }
            qualitySum += quality * decay;
            effectiveCount += decay;
        }

        return new RoleEvidence(sourceId(), qualitySum, effectiveCount, 1.0, since);
    }

    static double multiplier(final TaskResult result) {
        return switch (result) {
            case SUCCEEDED -> 1.0;
            case PARTIALLY -> 0.5;
            case FAILED -> 0.0;
        };
    }
}

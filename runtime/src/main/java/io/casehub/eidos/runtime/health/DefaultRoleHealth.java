package io.casehub.eidos.runtime.health;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.CapabilityHealth.ProbeContext;
import io.casehub.eidos.api.GraphDataSufficiency;
import io.casehub.eidos.api.RoleEvidence;
import io.casehub.eidos.api.RoleEvidenceSource;
import io.casehub.eidos.api.RoleHealth;
import io.casehub.eidos.api.SufficiencyLevel;
import io.casehub.eidos.api.WilsonConfidence;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class DefaultRoleHealth implements RoleHealth {

    private final Instance<RoleEvidenceSource>    evidenceSources;
    private final int                             windowDays;
    private final ThreadLocal<List<RoleEvidence>> lastEvidence = ThreadLocal.withInitial(List::of);

    @Inject
    public DefaultRoleHealth(final Instance<RoleEvidenceSource> evidenceSources,
                             @ConfigProperty(name = "casehub.eidos.role-health.window-days",
                                             defaultValue = "90") final int windowDays) {
        this.evidenceSources = evidenceSources;
        this.windowDays      = windowDays;
    }

    @Override
    public RoleStatus probe(final AgentDescriptor descriptor, final String roleName,
                            final ProbeContext context) {
        if (descriptor.roleProfile() == null || !descriptor.roleProfile().hasRole(roleName)) {
            return new RoleStatus.Undeclared(roleName);
        }

        final var evidenceList = new ArrayList<RoleEvidence>();
        for (final var source : evidenceSources) {
            evidenceList.add(source.evaluate(descriptor.agentId(), roleName,
                                             descriptor.tenancyId(), context));
        }
        lastEvidence.set(List.copyOf(evidenceList));

        double qTotal = 0.0;
        double nTotal = 0.0;
        for (final var evidence : evidenceList) {
            qTotal += evidence.weight() * evidence.qualitySum();
            nTotal += evidence.weight() * evidence.effectiveCount();
        }

        if (nTotal < 1.0) {
            return new RoleStatus.DeclaredFit(roleName);
        }

        final Instant since = Instant.now().minus(windowDays, ChronoUnit.DAYS);
        final Instant now   = Instant.now();
        final int     n     = Math.max(1, (int) Math.round(nTotal));
        if (GraphDataSufficiency.forCount(n, since, now, List.of()).level()
            == SufficiencyLevel.INSUFFICIENT) {
            return new RoleStatus.DeclaredFit(roleName);
        }

        final double confidence = WilsonConfidence.score(qTotal, n);
        final var    policy     = descriptor.roleProfile().changePolicy();

        if (confidence >= policy.minConfidence()) {
            return new RoleStatus.Fit(roleName, confidence);
        } else if (confidence >= policy.demotionConfidence()) {
            return new RoleStatus.Marginal(roleName, confidence, n);
        } else {
            return new RoleStatus.Unfit(roleName, confidence, (int) Math.round(nTotal - qTotal), n);
        }
    }

    @Override
    public List<RoleEvidence> lastProbeEvidence() {
        return lastEvidence.get();
    }
}

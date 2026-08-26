package io.casehub.eidos.runtime.selector;

import io.casehub.eidos.api.*;
import io.casehub.eidos.api.CapabilityHealth.CapabilityStatus;
import io.casehub.eidos.api.CapabilityHealth.ProbeContext;
import io.casehub.ledger.api.spi.TrustScoreSource;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalDouble;

@DefaultBean
@ApplicationScoped
public class SimpleAgentSelector implements AgentSelector {

    private final CapabilityHealth capabilityHealth;
    private final Instance<TrustScoreSource> trustSourceInstance;

    @ConfigProperty(name = "casehub.eidos.selector.trust-threshold", defaultValue = "0.0")
    double trustThreshold;

    @ConfigProperty(name = "casehub.eidos.selector.bootstrap-default-score", defaultValue = "0.5")
    double bootstrapDefaultScore;

    @Inject
    public SimpleAgentSelector(CapabilityHealth capabilityHealth,
                               Instance<TrustScoreSource> trustSourceInstance) {
        this.capabilityHealth = capabilityHealth;
        this.trustSourceInstance = trustSourceInstance;
    }

    SimpleAgentSelector(CapabilityHealth capabilityHealth,
                        Instance<TrustScoreSource> trustSourceInstance,
                        double trustThreshold,
                        double bootstrapDefaultScore) {
        this.capabilityHealth = capabilityHealth;
        this.trustSourceInstance = trustSourceInstance;
        this.trustThreshold = trustThreshold;
        this.bootstrapDefaultScore = bootstrapDefaultScore;
    }

    @Override
    public AgentSelection select(List<AgentMatch> candidates, SelectionContext context) {
        if (candidates.isEmpty()) {
            return new AgentSelection.NoneQualified("no candidates");
        }

        var healthy = filterHealthy(candidates, context);
        if (healthy.isEmpty()) {
            return new AgentSelection.NoneQualified(
                "all %d candidates unhealthy".formatted(candidates.size()));
        }

        var scored = scoreAndFilter(healthy, context);
        if (scored.isEmpty()) {
            return new AgentSelection.NoneQualified(
                "all %d candidates below trust threshold %.2f".formatted(
                    healthy.size(), trustThreshold));
        }

        scored.sort(Comparator
            .comparingDouble(ScoredMatch::score).reversed()
            .thenComparing(sm -> sm.match().resolvedCapability() != null
                ? sm.match().resolvedCapability().degree()
                : new MatchDegree.None())
            .thenComparing(sm -> sm.match().descriptor().agentId()));

        var best = scored.getFirst();
        return new AgentSelection.Selected(
            best.match().descriptor(),
            best.match().resolvedCapability(),
            best.score(),
            "highest trust score (simple selector)");
    }

    private List<AgentMatch> filterHealthy(List<AgentMatch> candidates,
                                            SelectionContext context) {
        var result = new ArrayList<AgentMatch>(candidates.size());
        for (var match : candidates) {
            var capTag = match.resolvedCapability() != null
                ? match.resolvedCapability().capability().name()
                : context.capabilityName();
            if (capTag == null) {
                result.add(match);
                continue;
            }
            var status = capabilityHealth.probe(
                match.descriptor(), capTag,
                ProbeContext.of(context.taskDomain()));
            if (status instanceof CapabilityStatus.Ready
                || status instanceof CapabilityStatus.Degraded
                || status instanceof CapabilityStatus.EpistemicallyWeak
                || status instanceof CapabilityStatus.BehavioralViolation) {
                result.add(match);
            }
        }
        return result;
    }

    private List<ScoredMatch> scoreAndFilter(List<AgentMatch> healthy,
                                              SelectionContext context) {
        var result = new ArrayList<ScoredMatch>(healthy.size());
        for (var match : healthy) {
            double score = resolveTrustScore(
                match.descriptor().agentId(), context.capabilityName());
            if (score >= trustThreshold) {
                result.add(new ScoredMatch(match, score));
            }
        }
        return result;
    }

    private double resolveTrustScore(String agentId, String capabilityName) {
        if (!trustSourceInstance.isResolvable()) {
            return 0.0;
        }
        var source = trustSourceInstance.get();
        var capScore = capabilityName != null
            ? source.capabilityScore(agentId, capabilityName)
            : OptionalDouble.empty();
        if (capScore.isPresent()) {
            return capScore.getAsDouble();
        }
        var globalScore = source.globalScore(agentId);
        return globalScore.orElse(bootstrapDefaultScore);
    }

    private record ScoredMatch(AgentMatch match, double score) {}
}

package io.casehub.eidos.runtime.health;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentGoal;
import io.casehub.eidos.api.GoalEvolution;
import io.casehub.eidos.api.GoalEvolutionResult;
import io.casehub.eidos.api.GoalOutcomeCounts;
import io.casehub.eidos.api.GoalPriority;
import io.casehub.eidos.runtime.preferences.GoalPreferenceKeys;
import io.casehub.platform.api.preferences.PreferenceProvider;
import io.casehub.platform.api.preferences.SettingsScope;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class DefaultGoalEvolution implements GoalEvolution {

    private final Instance<PreferenceProvider> preferenceProviderInstance;

    @Inject
    public DefaultGoalEvolution(final Instance<PreferenceProvider> preferenceProviderInstance) {
        this.preferenceProviderInstance = preferenceProviderInstance;
    }

    DefaultGoalEvolution() {
        this.preferenceProviderInstance = null;
    }

    @Override
    public GoalEvolutionResult evaluate(final AgentDescriptor descriptor,
                                         final Map<String, GoalOutcomeCounts> counts) {
        if (descriptor.goals().isEmpty() || counts.isEmpty()) {
            return new GoalEvolutionResult.Unchanged();
        }

        final double promoThreshold = promotionThreshold(descriptor.tenancyId());
        final int promoMinCount = promotionMinCount(descriptor.tenancyId());
        final double demoThreshold = demotionThreshold(descriptor.tenancyId());
        final int demoMinCount = demotionMinCount(descriptor.tenancyId());

        final var promoted = new ArrayList<String>();
        final var demoted = new ArrayList<String>();

        for (final var goal : descriptor.goals()) {
            if (goal.priority() != GoalPriority.SECONDARY) continue;
            final var c = counts.get(goal.name());
            if (c == null) continue;
            if (c.successCount() >= promoMinCount && c.successRate() >= promoThreshold) {
                promoted.add(goal.name());
            }
        }

        for (final var goal : descriptor.goals()) {
            if (goal.priority() != GoalPriority.PRIMARY) continue;
            final var c = counts.get(goal.name());
            if (c == null) continue;
            final double failureRate = 1.0 - c.successRate();
            if (c.failureCount() >= demoMinCount && failureRate >= demoThreshold) {
                demoted.add(goal.name());
            }
        }

        if (promoted.isEmpty() && demoted.isEmpty()) {
            return new GoalEvolutionResult.Unchanged();
        }

        final long remainingPrimary = descriptor.goals().stream()
            .filter(g -> g.priority() == GoalPriority.PRIMARY)
            .filter(g -> !demoted.contains(g.name()))
            .count();
        final long newPrimary = promoted.size();

        if (remainingPrimary + newPrimary == 0) {
            final var bestSecondary = descriptor.goals().stream()
                .filter(g -> g.priority() == GoalPriority.SECONDARY)
                .filter(g -> !promoted.contains(g.name()))
                .max(Comparator.comparingDouble(g -> {
                    final var c = counts.getOrDefault(g.name(), new GoalOutcomeCounts(0, 0));
                    return c.successRate();
                }));

            if (bestSecondary.isEmpty()) {
                final double decayFactor = decayFactor(descriptor.tenancyId());
                return new GoalEvolutionResult.Dampened(decayFactor);
            }
            promoted.add(bestSecondary.get().name());
        }

        final var newGoals = descriptor.goals().stream().map(g -> {
            if (promoted.contains(g.name())) {
                return new AgentGoal(g.name(), g.description(), GoalPriority.PRIMARY, g.visibility());
            }
            if (demoted.contains(g.name())) {
                return new AgentGoal(g.name(), g.description(), GoalPriority.SECONDARY, g.visibility());
            }
            return g;
        }).toList();

        return new GoalEvolutionResult.Evolved(newGoals, promoted, demoted);
    }

    private double promotionThreshold(final String tenancyId) {
        if (preferenceProviderInstance == null || preferenceProviderInstance.isUnsatisfied()) {
            return GoalPreferenceKeys.PROMOTION_THRESHOLD.defaultValue().value();
        }
        return preferenceProviderInstance.get()
            .resolve(SettingsScope.root(tenancyId))
            .getOrDefault(GoalPreferenceKeys.PROMOTION_THRESHOLD).value();
    }

    private int promotionMinCount(final String tenancyId) {
        if (preferenceProviderInstance == null || preferenceProviderInstance.isUnsatisfied()) {
            return GoalPreferenceKeys.PROMOTION_MIN_COUNT.defaultValue().value();
        }
        return preferenceProviderInstance.get()
            .resolve(SettingsScope.root(tenancyId))
            .getOrDefault(GoalPreferenceKeys.PROMOTION_MIN_COUNT).value();
    }

    private double demotionThreshold(final String tenancyId) {
        if (preferenceProviderInstance == null || preferenceProviderInstance.isUnsatisfied()) {
            return GoalPreferenceKeys.DEMOTION_THRESHOLD.defaultValue().value();
        }
        return preferenceProviderInstance.get()
            .resolve(SettingsScope.root(tenancyId))
            .getOrDefault(GoalPreferenceKeys.DEMOTION_THRESHOLD).value();
    }

    private int demotionMinCount(final String tenancyId) {
        if (preferenceProviderInstance == null || preferenceProviderInstance.isUnsatisfied()) {
            return GoalPreferenceKeys.DEMOTION_MIN_COUNT.defaultValue().value();
        }
        return preferenceProviderInstance.get()
            .resolve(SettingsScope.root(tenancyId))
            .getOrDefault(GoalPreferenceKeys.DEMOTION_MIN_COUNT).value();
    }

    private double decayFactor(final String tenancyId) {
        if (preferenceProviderInstance == null || preferenceProviderInstance.isUnsatisfied()) {
            return GoalPreferenceKeys.DECAY_FACTOR.defaultValue().value();
        }
        return preferenceProviderInstance.get()
            .resolve(SettingsScope.root(tenancyId))
            .getOrDefault(GoalPreferenceKeys.DECAY_FACTOR).value();
    }
}

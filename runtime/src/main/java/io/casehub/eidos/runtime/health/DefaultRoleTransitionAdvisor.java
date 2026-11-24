package io.casehub.eidos.runtime.health;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.RoleChangePolicy;
import io.casehub.eidos.api.RoleHealth.RoleStatus;
import io.casehub.eidos.api.RoleHoldCycleStore;
import io.casehub.eidos.api.RoleTransitionAdvisor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@ApplicationScoped
public class DefaultRoleTransitionAdvisor implements RoleTransitionAdvisor {

    private final RoleHoldCycleStore holdCycleStore;
    private final int windowDays;

    @Inject
    public DefaultRoleTransitionAdvisor(
            final RoleHoldCycleStore holdCycleStore,
            @ConfigProperty(name = "casehub.eidos.role-health.window-days",
                    defaultValue = "90") final int windowDays) {
        this.holdCycleStore = holdCycleStore;
        this.windowDays = windowDays;
    }

    @Override
    public TransitionAdvice evaluate(final AgentDescriptor descriptor,
                                     final RoleStatus currentStatus) {
        if (currentStatus instanceof RoleStatus.Undeclared) {
            throw new IllegalArgumentException(
                    "Cannot evaluate transition for undeclared role: " +
                    currentStatus.roleName());
        }

        final var policy = descriptor.roleProfile().changePolicy();
        if (policy.mode() == RoleChangePolicy.Mode.DISABLED) {
            return new TransitionAdvice.PolicyDisabled();
        }

        final String roleName = currentStatus.roleName();
        final String agentId = descriptor.agentId();
        final String tenancyId = descriptor.tenancyId();

        final var cycleState = holdCycleStore.get(agentId, roleName, tenancyId);
        if (cycleState.lastEvaluatedAt() != null) {
            final Instant staleThreshold = Instant.now().minus(windowDays, ChronoUnit.DAYS);
            if (cycleState.lastEvaluatedAt().isBefore(staleThreshold)) {
                holdCycleStore.reset(agentId, roleName, tenancyId);
            }
        }

        return switch (currentStatus) {
            case RoleStatus.DeclaredFit df -> {
                holdCycleStore.reset(agentId, roleName, tenancyId);
                yield new TransitionAdvice.Hold(roleName, 0.0, policy.mode());
            }
            case RoleStatus.Fit fit -> {
                holdCycleStore.reset(agentId, roleName, tenancyId);
                if (policy.mode() == RoleChangePolicy.Mode.ADVISORY) {
                    yield new TransitionAdvice.Hold(roleName, fit.confidence(), policy.mode());
                }
                yield new TransitionAdvice.PromotionWarranted(
                        roleName, fit.confidence(), policy.mode());
            }
            case RoleStatus.Marginal marginal ->
                new TransitionAdvice.Hold(roleName, marginal.confidence(), policy.mode());
            case RoleStatus.Unfit unfit -> {
                holdCycleStore.increment(agentId, roleName, tenancyId);
                final var updated = holdCycleStore.get(agentId, roleName, tenancyId);
                if (updated.consecutiveCycles() >= policy.minHoldCycles()) {
                    yield new TransitionAdvice.DemotionAdvised(
                            roleName, unfit.confidence(),
                            updated.consecutiveCycles(), policy.minHoldCycles(),
                            policy.mode());
                }
                yield new TransitionAdvice.Hold(roleName, unfit.confidence(), policy.mode());
            }
            case RoleStatus.Undeclared u ->
                throw new IllegalArgumentException("unreachable");
        };
    }
}

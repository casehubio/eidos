package io.casehub.eidos.api;

public interface RoleTransitionAdvisor {

    TransitionAdvice evaluate(AgentDescriptor descriptor,
                              RoleHealth.RoleStatus currentStatus);

    sealed interface TransitionAdvice permits
            TransitionAdvice.Hold,
            TransitionAdvice.PromotionWarranted,
            TransitionAdvice.DemotionAdvised,
            TransitionAdvice.PolicyDisabled {

        record Hold(String roleName, double confidence,
                    RoleChangePolicy.Mode policyMode) implements TransitionAdvice {}

        record PromotionWarranted(String roleName, double confidence,
                                  RoleChangePolicy.Mode policyMode) implements TransitionAdvice {}

        record DemotionAdvised(String roleName, double confidence,
                               int holdCyclesElapsed, int holdCyclesRequired,
                               RoleChangePolicy.Mode policyMode) implements TransitionAdvice {}

        record PolicyDisabled() implements TransitionAdvice {}
    }
}

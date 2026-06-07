package io.casehub.eidos.api;

import java.util.Optional;

/**
 * Behavioural disposition of an agent across open-String axes.
 * All String fields are self-declared and interpreted via the descriptor's vocabulary.
 * delegation is boolean because "can spawn sub-agents" is binary and platform-semantic.
 */
public record AgentDisposition(
        String socialOrient,
        String ruleFollowing,
        String riskAppetite,
        String autonomy,
        boolean delegation
) {
    public AgentDisposition {
        AgentDescriptorValidator.validateOptional("socialOrient", socialOrient,
            AgentDescriptorValidator.MAX_DISPOSITION_AXIS);
        AgentDescriptorValidator.validateOptional("ruleFollowing", ruleFollowing,
            AgentDescriptorValidator.MAX_DISPOSITION_AXIS);
        AgentDescriptorValidator.validateOptional("riskAppetite", riskAppetite,
            AgentDescriptorValidator.MAX_DISPOSITION_AXIS);
        AgentDescriptorValidator.validateOptional("autonomy", autonomy,
            AgentDescriptorValidator.MAX_DISPOSITION_AXIS);
    }

    public Optional<String> get(DispositionAxis axis) {
        return switch (axis) {
            case SOCIAL_ORIENTATION -> Optional.ofNullable(socialOrient);
            case RULE_FOLLOWING     -> Optional.ofNullable(ruleFollowing);
            case RISK_APPETITE      -> Optional.ofNullable(riskAppetite);
            case AUTONOMY           -> Optional.ofNullable(autonomy);
            case CONFLICT_MODE      -> Optional.empty();   // field added in Task 2
        };
    }
}

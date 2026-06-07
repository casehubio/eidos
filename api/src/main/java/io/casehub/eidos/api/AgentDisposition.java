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
        String conflictMode,
        boolean delegation
) {
    public AgentDisposition {
        AgentDescriptorValidator.validateOptional("socialOrient",  socialOrient,  AgentDescriptorValidator.MAX_DISPOSITION_AXIS);
        AgentDescriptorValidator.validateOptional("ruleFollowing", ruleFollowing, AgentDescriptorValidator.MAX_DISPOSITION_AXIS);
        AgentDescriptorValidator.validateOptional("riskAppetite",  riskAppetite,  AgentDescriptorValidator.MAX_DISPOSITION_AXIS);
        AgentDescriptorValidator.validateOptional("autonomy",      autonomy,      AgentDescriptorValidator.MAX_DISPOSITION_AXIS);
        AgentDescriptorValidator.validateOptional("conflictMode",  conflictMode,  AgentDescriptorValidator.MAX_DISPOSITION_AXIS);
    }

    public Optional<String> get(DispositionAxis axis) {
        return switch (axis) {
            case SOCIAL_ORIENTATION -> Optional.ofNullable(socialOrient);
            case RULE_FOLLOWING     -> Optional.ofNullable(ruleFollowing);
            case RISK_APPETITE      -> Optional.ofNullable(riskAppetite);
            case AUTONOMY           -> Optional.ofNullable(autonomy);
            case CONFLICT_MODE      -> Optional.ofNullable(conflictMode);
        };
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String socialOrient, ruleFollowing, riskAppetite, autonomy, conflictMode;
        private boolean delegation;

        public Builder socialOrient(String v)  { this.socialOrient  = v; return this; }
        public Builder ruleFollowing(String v) { this.ruleFollowing = v; return this; }
        public Builder riskAppetite(String v)  { this.riskAppetite  = v; return this; }
        public Builder autonomy(String v)      { this.autonomy      = v; return this; }
        public Builder conflictMode(String v)  { this.conflictMode  = v; return this; }
        public Builder delegation(boolean v)   { this.delegation    = v; return this; }

        public AgentDisposition build() {
            return new AgentDisposition(
                    socialOrient, ruleFollowing, riskAppetite, autonomy, conflictMode, delegation);
        }
    }
}

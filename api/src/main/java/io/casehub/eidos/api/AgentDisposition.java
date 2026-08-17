package io.casehub.eidos.api;

import java.util.List;

public record AgentDisposition(
        List<DispositionValue> socialOrient,
        List<DispositionValue> ruleFollowing,
        List<DispositionValue> riskAppetite,
        List<DispositionValue> autonomy,
        List<DispositionValue> conflictMode,
        boolean delegation,
        List<DispositionValue> dispositionProfile,
        List<DispositionValue> styleProfile
) {
    public AgentDisposition {
        socialOrient       = sanitizeAxis(socialOrient);
        ruleFollowing      = sanitizeAxis(ruleFollowing);
        riskAppetite       = sanitizeAxis(riskAppetite);
        autonomy           = sanitizeAxis(autonomy);
        conflictMode       = sanitizeAxis(conflictMode);
        dispositionProfile = dispositionProfile == null ? List.of() : List.copyOf(dispositionProfile);
        styleProfile       = styleProfile == null ? List.of() : List.copyOf(styleProfile);
    }

    private static List<DispositionValue> sanitizeAxis(List<DispositionValue> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    public static Builder builder() {return new Builder();}

    public List<DispositionValue> get(DispositionAxis axis) {
        return switch (axis) {
            case SOCIAL_ORIENTATION -> socialOrient;
            case RULE_FOLLOWING -> ruleFollowing;
            case RISK_APPETITE -> riskAppetite;
            case AUTONOMY -> autonomy;
            case CONFLICT_MODE -> conflictMode;
        };
    }

    public String primaryTerm(DispositionAxis axis) {
        var values = get(axis);
        return values.isEmpty() ? null : values.getFirst().term();
    }

    public static final class Builder {
        private List<DispositionValue> socialOrient       = List.of();
        private List<DispositionValue> ruleFollowing      = List.of();
        private List<DispositionValue> riskAppetite       = List.of();
        private List<DispositionValue> autonomy           = List.of();
        private List<DispositionValue> conflictMode       = List.of();
        private boolean                delegation;
        private List<DispositionValue> dispositionProfile = List.of();
        private List<DispositionValue> styleProfile       = List.of();

        public Builder socialOrient(String v) {
            this.socialOrient = v == null ? List.of() : List.of(DispositionValue.of(v));
            return this;
        }

        public Builder ruleFollowing(String v) {
            this.ruleFollowing = v == null ? List.of() : List.of(DispositionValue.of(v));
            return this;
        }

        public Builder riskAppetite(String v) {
            this.riskAppetite = v == null ? List.of() : List.of(DispositionValue.of(v));
            return this;
        }

        public Builder autonomy(String v) {
            this.autonomy = v == null ? List.of() : List.of(DispositionValue.of(v));
            return this;
        }

        public Builder conflictMode(String v) {
            this.conflictMode = v == null ? List.of() : List.of(DispositionValue.of(v));
            return this;
        }

        public Builder socialOrient(DispositionValue... values) {
            this.socialOrient = List.of(values);
            return this;
        }

        public Builder ruleFollowing(DispositionValue... values) {
            this.ruleFollowing = List.of(values);
            return this;
        }

        public Builder riskAppetite(DispositionValue... values) {
            this.riskAppetite = List.of(values);
            return this;
        }

        public Builder autonomy(DispositionValue... values) {
            this.autonomy = List.of(values);
            return this;
        }

        public Builder conflictMode(DispositionValue... values) {
            this.conflictMode = List.of(values);
            return this;
        }

        public Builder socialOrient(List<DispositionValue> v) {
            this.socialOrient = v == null ? List.of() : List.copyOf(v);
            return this;
        }

        public Builder ruleFollowing(List<DispositionValue> v) {
            this.ruleFollowing = v == null ? List.of() : List.copyOf(v);
            return this;
        }

        public Builder riskAppetite(List<DispositionValue> v) {
            this.riskAppetite = v == null ? List.of() : List.copyOf(v);
            return this;
        }

        public Builder autonomy(List<DispositionValue> v) {
            this.autonomy = v == null ? List.of() : List.copyOf(v);
            return this;
        }

        public Builder conflictMode(List<DispositionValue> v) {
            this.conflictMode = v == null ? List.of() : List.copyOf(v);
            return this;
        }

        public Builder delegation(boolean v) {
            this.delegation = v;
            return this;
        }

        public Builder dispositionProfile(DispositionValue... values) {
            this.dispositionProfile = List.of(values);
            return this;
        }

        public Builder dispositionProfile(List<DispositionValue> v) {
            this.dispositionProfile = v == null ? List.of() : List.copyOf(v);
            return this;
        }

        public Builder styleProfile(DispositionValue... values) {
            this.styleProfile = List.of(values);
            return this;
        }

        public Builder styleProfile(List<DispositionValue> v) {
            this.styleProfile = v == null ? List.of() : List.copyOf(v);
            return this;
        }

        public AgentDisposition build() {
            return new AgentDisposition(
                    socialOrient, ruleFollowing, riskAppetite, autonomy, conflictMode,
                    delegation, dispositionProfile, styleProfile);
        }
    }
}

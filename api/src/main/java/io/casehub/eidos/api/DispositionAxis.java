package io.casehub.eidos.api;

public enum DispositionAxis {
    SOCIAL_ORIENTATION,
    RULE_FOLLOWING,
    RISK_APPETITE,
    AUTONOMY,
    CONFLICT_MODE;

    public String jsonKey() {
        return switch (this) {
            case SOCIAL_ORIENTATION -> "socialOrient";
            case RULE_FOLLOWING     -> "ruleFollowing";
            case RISK_APPETITE      -> "riskAppetite";
            case AUTONOMY           -> "autonomy";
            case CONFLICT_MODE      -> "conflictMode";
        };
    }

    public String description() {
        return switch (this) {
            case SOCIAL_ORIENTATION ->
                "how collaborative or independent the agent is — whether it seeks input and " +
                "coordinates with others or acts independently";
            case RULE_FOLLOWING ->
                "how strictly the agent follows rules and conventions versus adapting its " +
                "approach to context";
            case RISK_APPETITE ->
                "how risk-tolerant or risk-averse the agent is — whether it favours bold " +
                "decisions under uncertainty or prioritises caution";
            case AUTONOMY ->
                "how self-directed versus directed-by-others the agent is — whether it takes " +
                "initiative or waits for instruction";
            case CONFLICT_MODE ->
                "how the agent approaches disagreement and conflict — whether it avoids, " +
                "accommodates, compromises, competes, or collaborates";
        };
    }

    public String displayName() {
        return switch (this) {
            case SOCIAL_ORIENTATION -> "Social orientation";
            case RULE_FOLLOWING -> "Rule following";
            case RISK_APPETITE -> "Risk appetite";
            case AUTONOMY -> "Autonomy";
            case CONFLICT_MODE -> "Conflict mode";
        };
    }

    public String lowPoleLabel() {
        return switch (this) {
            case SOCIAL_ORIENTATION -> "independent";
            case RULE_FOLLOWING -> "flexible";
            case RISK_APPETITE -> "cautious";
            case AUTONOMY -> "guided";
            case CONFLICT_MODE -> "avoiding";
        };
    }

    public String highPoleLabel() {
        return switch (this) {
            case SOCIAL_ORIENTATION -> "collaborative";
            case RULE_FOLLOWING -> "strict";
            case RISK_APPETITE -> "bold";
            case AUTONOMY -> "autonomous";
            case CONFLICT_MODE -> "competing";
        };
    }

}

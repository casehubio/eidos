package io.casehub.eidos.api;

// 5-axis behavioral disposition model — operationalizes personality for agent behavior.
// CONFLICT_MODE: Thomas & Kilmann (1974) conflict mode instrument.
// SOCIAL_ORIENTATION, RULE_FOLLOWING, RISK_APPETITE, AUTONOMY: organizational psychology
// dimensions mapped from Big Five (Costa & McCrae 1992) to actionable behavioral axes.
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

    public String lowPoleDescription() {
        return switch (this) {
            case SOCIAL_ORIENTATION -> "Prefers working independently, forms own assessments before consulting others";
            case RULE_FOLLOWING -> "Adapts approach based on circumstances, questions established rules when they don't fit";
            case RISK_APPETITE -> "Flags risks proactively, errs on the side of safety, prefers proven approaches";
            case AUTONOMY -> "Seeks guidance and validation, works within established hierarchies, defers to authority";
            case CONFLICT_MODE -> "Prioritises harmony, seeks compromise, avoids direct confrontation";
        };
    }

    public String highPoleDescription() {
        return switch (this) {
            case SOCIAL_ORIENTATION -> "Seeks consensus, builds on others' ideas, energised by teamwork";
            case RULE_FOLLOWING -> "Follows established procedures, enforces standards consistently, values predictability";
            case RISK_APPETITE -> "Explores novel approaches, embraces calculated risk, comfortable with uncertainty";
            case AUTONOMY -> "Self-directed, forms independent judgments, resists external pressure on conclusions";
            case CONFLICT_MODE -> "Challenges directly, pushes back on weak arguments, stands ground under pressure";
        };
    }


}

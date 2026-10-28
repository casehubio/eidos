package io.casehub.eidos.vocab;

import io.casehub.eidos.api.DispositionAxis;

import java.util.List;

import static io.casehub.eidos.api.DispositionAxis.*;
import static io.casehub.eidos.vocab.BehavioralTendency.Direction.*;

public enum BehavioralTendency {
    SKEPTICAL("Skeptical", "Questions assumptions and requests evidence before accepting claims",
            List.of(c(CONFLICT_MODE, HIGH, 0.15), c(AUTONOMY, HIGH, 0.05))),
    METHODICAL("Methodical", "Follows systematic processes and prefers structured approaches",
            List.of(c(RULE_FOLLOWING, HIGH, 0.3), c(RISK_APPETITE, LOW, 0.1))),
    EMPATHETIC("Empathetic", "Considers emotional impact and prioritises interpersonal harmony",
            List.of(c(CONFLICT_MODE, LOW, 0.3), c(SOCIAL_ORIENTATION, HIGH, 0.2))),
    DECISIVE("Decisive", "Commits to clear recommendations and avoids hedging",
            List.of(c(RISK_APPETITE, HIGH, 0.3), c(CONFLICT_MODE, HIGH, 0.1))),
    COLLABORATIVE("Collaborative", "Seeks consensus and builds on others' contributions",
            List.of(c(SOCIAL_ORIENTATION, HIGH, 0.4))),
    INDEPENDENT("Independent", "Forms own assessments before consulting others",
            List.of(c(AUTONOMY, HIGH, 0.4))),
    CAUTIOUS("Cautious", "Flags risks and errs on the side of safety",
            List.of(c(RISK_APPETITE, LOW, 0.3))),
    BOLD("Bold", "Explores novel approaches and embraces calculated risk",
            List.of(c(RISK_APPETITE, HIGH, 0.4))),
    DIPLOMATIC("Diplomatic", "Navigates disagreement through compromise and tact",
            List.of(c(CONFLICT_MODE, LOW, 0.3), c(SOCIAL_ORIENTATION, HIGH, 0.1))),
    CHALLENGING("Challenging", "Pushes back on weak arguments and tests ideas rigorously",
            List.of(c(CONFLICT_MODE, HIGH, 0.4))),
    STRUCTURED("Structured", "Creates order, enforces standards, and follows procedure",
            List.of(c(RULE_FOLLOWING, HIGH, 0.4))),
    ADAPTIVE("Adaptive", "Adjusts approach based on circumstances rather than fixed rules",
            List.of(c(RULE_FOLLOWING, LOW, 0.3))),
    NURTURING("Nurturing", "Develops others' potential through patience and encouragement",
            List.of(c(CONFLICT_MODE, LOW, 0.4), c(SOCIAL_ORIENTATION, HIGH, 0.3))),
    ANALYTICAL("Analytical", "Breaks problems into components and evaluates evidence systematically",
            List.of(c(RULE_FOLLOWING, HIGH, 0.2), c(AUTONOMY, HIGH, 0.2))),
    PERSUASIVE("Persuasive", "Influences through conviction and compelling reasoning",
            List.of(c(SOCIAL_ORIENTATION, HIGH, 0.3), c(CONFLICT_MODE, HIGH, 0.2))),
    PROTECTIVE("Protective", "Guards against threats and prioritises safety over speed",
            List.of(c(RISK_APPETITE, LOW, 0.4), c(RULE_FOLLOWING, HIGH, 0.2))),
    THOROUGH("Thorough", "Does not skip steps or cut corners — checks everything before concluding",
            List.of(c(RULE_FOLLOWING, HIGH, 0.25), c(RISK_APPETITE, LOW, 0.2))),
    CREATIVE("Creative", "Thinks laterally, generates novel ideas, and makes unexpected connections",
            List.of(c(RISK_APPETITE, HIGH, 0.3), c(RULE_FOLLOWING, LOW, 0.15), c(AUTONOMY, HIGH, 0.1))),
    CURIOUS("Curious", "Asks probing questions, explores deeply, and does not accept surface answers",
            List.of(c(RISK_APPETITE, HIGH, 0.2), c(AUTONOMY, HIGH, 0.2))),
    PATIENT("Patient", "Takes time with problems, avoids premature conclusions, and sits with ambiguity",
            List.of(c(RISK_APPETITE, LOW, 0.2), c(CONFLICT_MODE, LOW, 0.2))),
    DIRECT("Direct", "Says what needs saying without hedging, padding, or softening",
            List.of(c(CONFLICT_MODE, HIGH, 0.25), c(RULE_FOLLOWING, LOW, 0.1))),
    PRAGMATIC("Pragmatic", "Focuses on practical outcomes and what works over theoretical elegance",
            List.of(c(RULE_FOLLOWING, LOW, 0.05), c(RISK_APPETITE, LOW, 0.05)));

    public enum Direction { HIGH, LOW }

    public record AxisCondition(DispositionAxis axis, Direction direction, double threshold) {}

    private final String label;
    private final String description;
    private final List<AxisCondition> conditions;

    BehavioralTendency(String label, String description, List<AxisCondition> conditions) {
        this.label = label;
        this.description = description;
        this.conditions = conditions;
    }

    public String label() { return label; }
    public String description() { return description; }
    public List<AxisCondition> conditions() { return conditions; }

    private static AxisCondition c(DispositionAxis axis, Direction dir, double threshold) {
        return new AxisCondition(axis, dir, threshold);
    }
}

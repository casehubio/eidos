package io.casehub.eidos.api;

import java.util.List;

public record GoalContext(
        String description,
        List<String> subGoals,
        String caseRef
) {
    public GoalContext {
        subGoals = subGoals != null ? subGoals : List.of();
    }

    public static GoalContext of(final String description) {
        return new GoalContext(description, List.of(), null);
    }
}

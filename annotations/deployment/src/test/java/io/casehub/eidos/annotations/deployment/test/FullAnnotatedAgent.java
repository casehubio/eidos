package io.casehub.eidos.annotations.deployment.test;

import io.casehub.eidos.annotations.*;
import io.casehub.eidos.api.*;

@Identity(id = "full-agent", name = "Full Agent", slot = "analyst",
          jurisdiction = "EU", briefing = "Full featured")
@Disposition(socialOrient = "collaborative", ruleFollowing = "strict",
             riskAppetite = "cautious", autonomy = "guided",
             conflictMode = "accommodating")
@Discoverable(capabilities = {"analysis", "review"})
@AgentGoals({
    @AgentGoalDef(name = "accurate", description = "Be accurate",
                  priority = GoalPriority.PRIMARY, capabilities = {"analysis"}),
    @AgentGoalDef(name = "thorough", description = "Be thorough",
                  priority = GoalPriority.SECONDARY)
})
@AgentConstraints({
    @AgentConstraintDef(name = "no-advice", description = "No binding advice",
                        severity = ConstraintSeverity.HARD),
    @AgentConstraintDef(name = "cite-sources", description = "Cite sources",
                        severity = ConstraintSeverity.SOFT, visibility = Visibility.PRIVATE)
})
public interface FullAnnotatedAgent {}

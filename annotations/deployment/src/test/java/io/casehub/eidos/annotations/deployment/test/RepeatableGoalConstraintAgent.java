package io.casehub.eidos.annotations.deployment.test;

import io.casehub.eidos.annotations.AgentConstraintDef;
import io.casehub.eidos.annotations.AgentGoalDef;
import io.casehub.eidos.annotations.Identity;
import io.casehub.eidos.api.ConstraintSeverity;
import io.casehub.eidos.api.GoalPriority;

@Identity(id = "repeatable-agent", name = "Repeatable Agent", slot = "tester")
@AgentGoalDef(name = "speed", description = "Respond quickly", priority = GoalPriority.PRIMARY)
@AgentGoalDef(name = "clarity", description = "Be clear", priority = GoalPriority.SECONDARY)
@AgentConstraintDef(name = "no-pii", description = "Never expose PII", severity = ConstraintSeverity.HARD)
@AgentConstraintDef(name = "log-actions", description = "Log all actions", severity = ConstraintSeverity.SOFT)
public interface RepeatableGoalConstraintAgent {}

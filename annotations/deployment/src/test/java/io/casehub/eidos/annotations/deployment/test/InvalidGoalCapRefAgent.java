package io.casehub.eidos.annotations.deployment.test;

import io.casehub.eidos.annotations.*;
import io.casehub.eidos.api.*;

@Identity(slot = "bad-agent")
@Discoverable(capabilities = {"analysis"})
@AgentGoals({
    @AgentGoalDef(name = "goal1", description = "A goal",
                  capabilities = {"nonexistent-capability"})
})
public interface InvalidGoalCapRefAgent {}

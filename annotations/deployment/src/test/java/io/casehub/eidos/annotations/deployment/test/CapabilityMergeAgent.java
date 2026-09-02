package io.casehub.eidos.annotations.deployment.test;

import io.casehub.eidos.annotations.*;
import io.casehub.eidos.api.Discoverable;

@Identity(slot = "merge-agent", briefing = "Agent with merged capabilities")
@Discoverable(capabilities = {"simple-cap"})
@AgentCapabilityDef(name = "rich-cap", description = "A rich capability", qualityHint = 0.9)
public interface CapabilityMergeAgent {}

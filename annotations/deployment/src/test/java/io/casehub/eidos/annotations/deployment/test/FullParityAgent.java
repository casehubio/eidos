package io.casehub.eidos.annotations.deployment.test;

import io.casehub.eidos.annotations.*;
import io.casehub.eidos.api.*;

@Identity(id = "parity-agent", name = "Parity Agent", slot = "analyst",
          vocabulary = "urn:casehub:vocab:svo",
          dispositionVocabulary = "urn:casehub:vocab:conscientiousness",
          provider = "test-provider", modelFamily = "test-model",
          modelVersion = "v1", weightsFingerprint = "sha256:parity",
          jurisdiction = "EU", dataHandlingPolicy = "gdpr",
          briefing = "Full parity test agent", version = "1.0")
@Disposition(socialOrient = "collaborative", ruleFollowing = "strict",
             riskAppetite = "cautious", autonomy = "guided",
             conflictMode = "accommodating", delegation = true,
             dispositionProfile = {
                 @DispositionWeight(value = "collaborative", weight = 0.8),
                 @DispositionWeight(value = "analytical", weight = 0.4)
             },
             axisVocabularies = {
                 @AxisVocabulary(axis = DispositionAxis.CONFLICT_MODE,
                                uri = "urn:casehub:vocab:thomas-kilmann")
             })
@AgentCapabilityDef(name = "cap-a", description = "Capability A",
    qualityHint = 0.9, latencyHintP50Ms = 2000, costHint = "low",
    inputTypes = {"text/plain"}, outputTypes = {"application/json"},
    tags = {"tag1"},
    epistemicDomains = {@EpistemicDomain(value = "domain-a", score = 0.95)},
    excludedDomains = {"domain-x"})
@Discoverable(capabilities = {"cap-b"})
@AgentGoals({
    @AgentGoalDef(name = "goal-1", description = "Primary goal",
                  priority = GoalPriority.PRIMARY, capabilities = {"cap-a"}),
    @AgentGoalDef(name = "goal-2", description = "Secondary goal",
                  priority = GoalPriority.SECONDARY)
})
@AgentConstraints({
    @AgentConstraintDef(name = "constraint-1", description = "Hard constraint",
                        severity = ConstraintSeverity.HARD),
    @AgentConstraintDef(name = "constraint-2", description = "Private soft",
                        severity = ConstraintSeverity.SOFT, visibility = Visibility.PRIVATE)
})
@AgentTemplateRef(id = "safety-primer",
    args = {@TemplateArg(key = "domain", value = "legal")})
public interface FullParityAgent {}

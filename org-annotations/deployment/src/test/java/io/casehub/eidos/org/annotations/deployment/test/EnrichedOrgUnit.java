package io.casehub.eidos.org.annotations.deployment.test;

import io.casehub.eidos.annotations.AgentCapabilityDef;
import io.casehub.eidos.annotations.AgentConstraintDef;
import io.casehub.eidos.annotations.AgentGoalDef;
import io.casehub.eidos.annotations.EpistemicDomain;
import io.casehub.eidos.api.ConstraintSeverity;
import io.casehub.eidos.api.GoalPriority;
import io.casehub.eidos.api.Visibility;
import io.casehub.eidos.org.annotations.AttestationGrantDef;
import io.casehub.eidos.org.annotations.OrgMemberDef;
import io.casehub.eidos.org.annotations.OrgMembers;
import io.casehub.eidos.org.annotations.OrgRelationshipDef;
import io.casehub.eidos.org.annotations.OrgRelationships;
import io.casehub.eidos.org.annotations.OrgUnit;
import io.casehub.eidos.org.annotations.Supervises;

@OrgUnit(
    id = "enriched-unit",
    name = "Enriched Unit",
    kind = "squad",
    capabilities = {
        @AgentCapabilityDef(name = "code-review", description = "Reviews code changes",
            qualityHint = 0.9, latencyHintP50Ms = 5000,
            epistemicDomains = @EpistemicDomain(value = "java", score = 0.95))
    },
    goals = {
        @AgentGoalDef(name = "quality-gate", description = "Ensure code quality standards",
            priority = GoalPriority.PRIMARY, visibility = Visibility.PUBLIC),
        @AgentGoalDef(name = "knowledge-share", description = "Share domain knowledge",
            priority = GoalPriority.SECONDARY)
    },
    constraints = {
        @AgentConstraintDef(name = "no-force-push", description = "Never force push to main",
            severity = ConstraintSeverity.HARD),
        @AgentConstraintDef(name = "review-sla", description = "Respond within 4 hours",
            severity = ConstraintSeverity.SOFT, visibility = Visibility.PRIVATE)
    }
)
@OrgMembers({
    @OrgMemberDef(agentId = "lead-1", role = "lead"),
    @OrgMemberDef(agentId = "reviewer-1", role = "reviewer")
})
@Supervises(source = "lead-1", target = "reviewer-1",
    scope = "code-review", scopeDomain = "backend", scopeCondition = "pr-open")
@OrgRelationships({
    @OrgRelationshipDef(source = "reviewer-1", target = "lead-1", kind = "ESCALATES_TO",
        scope = "code-review", scopeDomain = "security",
        attestation = @AttestationGrantDef(
            dimensions = {"quality", "latency"},
            capabilityScope = {"code-review"},
            signalTypes = {"COMPLIANT", "VIOLATED"}
        ))
})
public interface EnrichedOrgUnit {}

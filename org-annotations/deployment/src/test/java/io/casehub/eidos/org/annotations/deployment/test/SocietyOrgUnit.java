package io.casehub.eidos.org.annotations.deployment.test;

import io.casehub.eidos.org.annotations.FormationRulesDef;
import io.casehub.eidos.org.annotations.GroupPolicyDef;
import io.casehub.eidos.org.annotations.MissionSpecDef;
import io.casehub.eidos.org.annotations.OrgMemberDef;
import io.casehub.eidos.org.annotations.OrgMembers;
import io.casehub.eidos.org.annotations.OrgUnit;
import io.casehub.eidos.org.annotations.PolicyExpectation;
import io.casehub.eidos.org.annotations.RolePolicyDef;
import io.casehub.eidos.org.annotations.RoleSpecDef;

@OrgUnit(
    id = "dev-guild",
    name = "Developer Guild",
    kind = "guild",
    topology = "mesh",
    topologyVocabulary = "urn:casehub:vocab:topology",
    authorityModel = "DISTRIBUTED",
    selfDetermination = 0.8,
    policy = @GroupPolicyDef(
        normativePriority = 2,
        normSpecificity = "DOMAIN",
        dissolutionDrainTimeoutMinutes = 15,
        formationRules = @FormationRulesDef(
            allowedPaths = {"BOTTOM_UP", "EMERGENCE"},
            minGroupSize = 3,
            maxGroupSize = 10,
            allowSubGroups = true,
            proposalQuorum = 0.6
        )
    ),
    roleSpecs = {
        @RoleSpecDef(
            roleName = "lead",
            roleVocabulary = "urn:casehub:vocab:roles",
            minCardinality = 1,
            maxCardinality = 1,
            obligations = {"review-output", "approve-plan"},
            permissions = {"provision-agent", "change-topology"},
            policies = {
                @RolePolicyDef(capabilityName = "code-review", kind = "OBLIGATION",
                    expectations = @PolicyExpectation(key = "turnaround", value = "24h")),
                @RolePolicyDef(capabilityName = "merge-authority", kind = "PERMISSION")
            }
        ),
        @RoleSpecDef(
            roleName = "contributor",
            minCardinality = 2,
            maxCardinality = -1,
            incompatibleWith = {"lead"},
            policies = @RolePolicyDef(capabilityName = "code-review", kind = "ELIGIBILITY")
        )
    },
    missions = {
        @MissionSpecDef(missionName = "platform-api-v2", description = "Deliver v2 of the platform API"),
        @MissionSpecDef(missionName = "tech-debt-reduction", description = "Reduce critical tech debt by 30%")
    }
)
@OrgMembers({
    @OrgMemberDef(agentId = "alice", role = "lead"),
    @OrgMemberDef(agentId = "bob", role = "contributor"),
    @OrgMemberDef(agentId = "carol", role = "contributor")
})
public interface SocietyOrgUnit {}

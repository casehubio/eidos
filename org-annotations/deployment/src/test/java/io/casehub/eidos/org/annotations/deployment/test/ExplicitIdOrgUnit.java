package io.casehub.eidos.org.annotations.deployment.test;

import io.casehub.eidos.org.annotations.OrgMemberDef;
import io.casehub.eidos.org.annotations.OrgMembers;
import io.casehub.eidos.org.annotations.OrgRelationshipDef;
import io.casehub.eidos.org.annotations.OrgRelationships;
import io.casehub.eidos.org.annotations.OrgUnit;

@OrgUnit(id = "oversight", name = "Oversight Chain", kind = "supervision-hierarchy")
@OrgMembers({
    @OrgMemberDef(agentId = "boot", role = "root-watchdog"),
    @OrgMemberDef(agentId = "deacon", role = "cross-rig-watchdog")
})
@OrgRelationships({
    @OrgRelationshipDef(source = "boot", target = "deacon", kind = "SUPERVISES"),
    @OrgRelationshipDef(source = "polecat-1", target = "polecat-2", kind = "BACKS_UP",
                        scope = "code-analysis")
})
public interface ExplicitIdOrgUnit {}

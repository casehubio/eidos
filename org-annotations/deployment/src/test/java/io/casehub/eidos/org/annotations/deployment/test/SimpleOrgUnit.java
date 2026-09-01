package io.casehub.eidos.org.annotations.deployment.test;

import io.casehub.eidos.org.annotations.OrgMemberDef;
import io.casehub.eidos.org.annotations.OrgMembers;
import io.casehub.eidos.org.annotations.OrgUnit;
import io.casehub.eidos.org.annotations.Supervises;

@OrgUnit(kind = "rig")
@OrgMembers({
    @OrgMemberDef(agentId = "witness-1", role = "witness"),
    @OrgMemberDef(agentId = "polecat-1", role = "worker"),
    @OrgMemberDef(agentId = "polecat-2", role = "worker")
})
@Supervises(source = "witness-1", target = "polecat-1")
@Supervises(source = "witness-1", target = "polecat-2")
public interface SimpleOrgUnit {}

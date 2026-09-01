package io.casehub.eidos.org.annotations.deployment.test;

import io.casehub.eidos.org.annotations.OrgMemberDef;
import io.casehub.eidos.org.annotations.OrgMembers;
import io.casehub.eidos.org.annotations.OrgUnit;

@OrgUnit(id = "sub-team", name = "Sub Team", kind = "squad", parentUnit = "minimal-org-unit")
@OrgMembers({
    @OrgMemberDef(agentId = "agent-1")
})
public interface HierarchyChildUnit {}

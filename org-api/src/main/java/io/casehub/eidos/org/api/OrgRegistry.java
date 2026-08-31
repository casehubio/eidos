package io.casehub.eidos.org.api;

import java.util.List;
import java.util.Optional;

public interface OrgRegistry {

    void registerUnit(OrganizationalUnit unit);
    void removeUnit(String unitId, String tenancyId);
    Optional<OrganizationalUnit> findUnit(String unitId, String tenancyId);
    List<OrganizationalUnit> findUnits(OrgQuery query);
    List<OrganizationalUnit> childUnits(String parentUnitId, String tenancyId);
    List<OrganizationalUnit> ancestorUnits(String unitId, String tenancyId);

    List<OrganizationalUnit> unitsFor(String agentId, String tenancyId);
    List<Membership> membersOf(String unitId, String tenancyId);

    void addRelationship(AgentRelationship relationship);
    void removeRelationship(String sourceAgentId, String targetAgentId,
                            RelationshipKind kind, String tenancyId);
    List<AgentRelationship> relationshipsFrom(String agentId, String tenancyId);
    List<AgentRelationship> relationshipsTo(String agentId, String tenancyId);

    List<AgentRelationship> supervisors(String agentId, String tenancyId);
    List<AgentRelationship> subordinates(String agentId, String tenancyId);
    List<AgentRelationship> escalationPath(String agentId, String tenancyId);
}

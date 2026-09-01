package io.casehub.eidos.org.memory;

import io.casehub.eidos.org.api.AgentRelationship;
import io.casehub.eidos.org.api.Membership;
import io.casehub.eidos.org.api.OrgQuery;
import io.casehub.eidos.org.api.OrgRegistry;
import io.casehub.eidos.org.api.OrgValidationException;
import io.casehub.eidos.org.api.OrganizationalUnit;
import io.casehub.eidos.org.api.RelationshipKind;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Alternative
@Priority(1)
@ApplicationScoped
public class InMemoryOrgRegistry implements OrgRegistry {

    private final Map<String, OrganizationalUnit> units = new ConcurrentHashMap<>();
    private final List<AgentRelationship> relationships = Collections.synchronizedList(new ArrayList<>());

    private String key(String id, String tenancyId) {
        return tenancyId + ":" + id;
    }

    @Override
    public void registerUnit(OrganizationalUnit unit) {
        if (unit.parentUnitId() != null) {
            detectCycle(unit.unitId(), unit.parentUnitId(), unit.tenancyId());
        }
        units.put(key(unit.unitId(), unit.tenancyId()), unit);
    }

    @Override
    public void removeUnit(String unitId, String tenancyId) {
        units.remove(key(unitId, tenancyId));
    }

    @Override
    public Optional<OrganizationalUnit> findUnit(String unitId, String tenancyId) {
        return Optional.ofNullable(units.get(key(unitId, tenancyId)));
    }

    @Override
    public List<OrganizationalUnit> findUnits(OrgQuery query) {
        return units.values().stream()
            .filter(u -> u.tenancyId().equals(query.tenancyId()))
            .filter(u -> query.kind() == null || query.kind().equals(u.kind()))
            .filter(u -> query.parentUnitId() == null ||
                         Objects.equals(query.parentUnitId(), u.parentUnitId()))
            .toList();
    }

    @Override
    public List<OrganizationalUnit> childUnits(String parentUnitId, String tenancyId) {
        return units.values().stream()
            .filter(u -> u.tenancyId().equals(tenancyId))
            .filter(u -> parentUnitId.equals(u.parentUnitId()))
            .toList();
    }

    @Override
    public List<OrganizationalUnit> ancestorUnits(String unitId, String tenancyId) {
        var ancestors = new ArrayList<OrganizationalUnit>();
        var visited = new HashSet<String>();
        var current = findUnit(unitId, tenancyId).orElse(null);
        while (current != null && current.parentUnitId() != null) {
            if (!visited.add(current.parentUnitId())) break;
            current = findUnit(current.parentUnitId(), tenancyId).orElse(null);
            if (current != null) ancestors.add(current);
        }
        return ancestors;
    }

    @Override
    public List<OrganizationalUnit> unitsFor(String agentId, String tenancyId) {
        return units.values().stream()
            .filter(u -> u.tenancyId().equals(tenancyId))
            .filter(u -> u.hasMember(agentId))
            .toList();
    }

    @Override
    public List<Membership> membersOf(String unitId, String tenancyId) {
        return findUnit(unitId, tenancyId)
            .map(OrganizationalUnit::members)
            .orElse(List.of());
    }

    @Override
    public void addRelationship(AgentRelationship relationship) {
        relationships.add(relationship);
    }

    @Override
    public void removeRelationship(String sourceAgentId, String targetAgentId,
                                   RelationshipKind kind, String tenancyId) {
        relationships.removeIf(r ->
            r.sourceAgentId().equals(sourceAgentId) &&
            r.targetAgentId().equals(targetAgentId) &&
            r.kind() == kind &&
            r.tenancyId().equals(tenancyId));
    }

    @Override
    public List<AgentRelationship> relationshipsFrom(String agentId, String tenancyId) {
        return relationships.stream()
            .filter(r -> r.sourceAgentId().equals(agentId) && r.tenancyId().equals(tenancyId))
            .toList();
    }

    @Override
    public List<AgentRelationship> relationshipsTo(String agentId, String tenancyId) {
        return relationships.stream()
            .filter(r -> r.targetAgentId().equals(agentId) && r.tenancyId().equals(tenancyId))
            .toList();
    }

    @Override
    public List<AgentRelationship> supervisors(String agentId, String tenancyId) {
        return relationshipsTo(agentId, tenancyId).stream()
            .filter(r -> r.kind() == RelationshipKind.SUPERVISES)
            .toList();
    }

    @Override
    public List<AgentRelationship> subordinates(String agentId, String tenancyId) {
        return relationshipsFrom(agentId, tenancyId).stream()
            .filter(r -> r.kind() == RelationshipKind.SUPERVISES)
            .toList();
    }

    @Override
    public List<AgentRelationship> escalationPath(String agentId, String tenancyId) {
        var path = new ArrayList<AgentRelationship>();
        var visited = new HashSet<String>();
        var current = agentId;
        while (visited.add(current)) {
            String cur = current;
            var escalation = relationships.stream()
                .filter(r -> r.sourceAgentId().equals(cur) &&
                             r.tenancyId().equals(tenancyId) &&
                             r.kind() == RelationshipKind.ESCALATES_TO)
                .findFirst();
            if (escalation.isEmpty()) break;
            path.add(escalation.get());
            current = escalation.get().targetAgentId();
        }
        return path;
    }

    private void detectCycle(String unitId, String parentId, String tenancyId) {
        var visited = new HashSet<String>();
        visited.add(unitId);
        var current = parentId;
        while (current != null) {
            if (!visited.add(current)) {
                throw new OrgValidationException("parentUnitId",
                    "cycle detected: " + unitId + " → " + current);
            }
            var parent = units.get(key(current, tenancyId));
            current = parent != null ? parent.parentUnitId() : null;
        }
    }
}

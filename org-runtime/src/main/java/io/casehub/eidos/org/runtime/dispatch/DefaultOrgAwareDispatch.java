package io.casehub.eidos.org.runtime.dispatch;

import io.casehub.eidos.api.AgentMatch;
import io.casehub.eidos.api.AgentRegistry;
import io.casehub.eidos.api.CapabilityHealth;
import io.casehub.eidos.api.CapabilityHealth.CapabilityStatus;
import io.casehub.eidos.api.CapabilityHealth.ProbeContext;
import io.casehub.eidos.api.OrgAwareDispatch;
import io.casehub.eidos.org.api.OrgRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class DefaultOrgAwareDispatch implements OrgAwareDispatch {

    private final OrgRegistry orgRegistry;
    private final AgentRegistry agentRegistry;
    private final CapabilityHealth capabilityHealth;

    @Inject
    public DefaultOrgAwareDispatch(OrgRegistry orgRegistry,
                                   AgentRegistry agentRegistry,
                                   CapabilityHealth capabilityHealth) {
        this.orgRegistry = orgRegistry;
        this.agentRegistry = agentRegistry;
        this.capabilityHealth = capabilityHealth;
    }

    @Override
    public Optional<AgentMatch> escalate(String agentId, String capabilityName,
                                         String tenancyId) {
        var path = orgRegistry.escalationPath(agentId, tenancyId);
        for (var step : path) {
            var targetId = step.targetAgentId();
            var descriptor = agentRegistry.findById(targetId, tenancyId);
            if (descriptor.isEmpty()) {
                continue;
            }
            var status = capabilityHealth.probe(
                    descriptor.get(), capabilityName, ProbeContext.of(null));
            if (status instanceof CapabilityStatus.Ready) {
                return Optional.of(new AgentMatch(descriptor.get(), null));
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean canDelegateTo(String fromAgentId, String toAgentId,
                                 String tenancyId) {
        var subordinates = orgRegistry.subordinates(fromAgentId, tenancyId);
        if (subordinates.stream().anyMatch(r -> r.targetAgentId().equals(toAgentId))) {
            return true;
        }
        var fromUnits = orgRegistry.unitsFor(fromAgentId, tenancyId);
        for (var unit : fromUnits) {
            var members = orgRegistry.membersOf(unit.unitId(), tenancyId);
            if (members.stream().anyMatch(m -> m.agentId().equals(toAgentId))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<String> teamMembers(String agentId, String tenancyId) {
        return orgRegistry.unitsFor(agentId, tenancyId).stream()
                .flatMap(u -> orgRegistry.membersOf(u.unitId(), tenancyId).stream())
                .map(m -> m.agentId())
                .filter(id -> !id.equals(agentId))
                .distinct()
                .toList();
    }
}

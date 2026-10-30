package io.casehub.eidos.org.runtime.prompt;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentPromptContext;
import io.casehub.eidos.api.PromptBlock;
import io.casehub.eidos.api.spi.PromptContributor;
import io.casehub.eidos.org.api.OrgRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class OrgPromptContributor implements PromptContributor {

    private final Instance<OrgRegistry> registryInstance;

    @Inject
    public OrgPromptContributor(Instance<OrgRegistry> registryInstance) {
        this.registryInstance = registryInstance;
    }

    @Override
    public List<PromptBlock> contribute(AgentDescriptor descriptor, AgentPromptContext context) {
        if (!registryInstance.isResolvable()) {
            return List.of();
        }
        var registry = registryInstance.get();
        var agentId = descriptor.agentId();
        var tenancyId = descriptor.tenancyId();

        var sb = new StringBuilder();
        appendUnits(sb, registry, agentId, tenancyId);
        appendSupervisors(sb, registry, agentId, tenancyId);
        appendSubordinates(sb, registry, agentId, tenancyId);
        appendEscalationPath(sb, registry, agentId, tenancyId);
        appendTeamPeers(sb, registry, agentId, tenancyId);

        if (sb.isEmpty()) {
            return List.of();
        }
        return List.of(PromptBlock.cognitive("ORG_CONTEXT", sb.toString().strip()));
    }

    private void appendUnits(StringBuilder sb, OrgRegistry registry,
                             String agentId, String tenancyId) {
        var units = registry.unitsFor(agentId, tenancyId);
        for (var unit : units) {
            sb.append("You are a member of \"").append(unit.name()).append("\"");
            if (unit.kind() != null && !unit.kind().isBlank()) {
                sb.append(" (").append(unit.kind()).append(")");
            }
            sb.append(".\n");
        }
    }

    private void appendSupervisors(StringBuilder sb, OrgRegistry registry,
                                   String agentId, String tenancyId) {
        var supervisors = registry.supervisors(agentId, tenancyId);
        if (!supervisors.isEmpty()) {
            var names = supervisors.stream()
                    .map(r -> r.sourceAgentId())
                    .toList();
            sb.append("Your supervisor");
            if (names.size() == 1) {
                sb.append(" is ").append(names.getFirst());
            } else {
                sb.append("s are ").append(String.join(", ", names));
            }
            sb.append(".\n");
        }
    }

    private void appendSubordinates(StringBuilder sb, OrgRegistry registry,
                                    String agentId, String tenancyId) {
        var subordinates = registry.subordinates(agentId, tenancyId);
        if (!subordinates.isEmpty()) {
            var names = subordinates.stream()
                    .map(r -> r.targetAgentId())
                    .toList();
            sb.append("You can delegate work to: ")
              .append(String.join(", ", names)).append(".\n");
        }
    }

    private void appendEscalationPath(StringBuilder sb, OrgRegistry registry,
                                      String agentId, String tenancyId) {
        var path = registry.escalationPath(agentId, tenancyId);
        if (!path.isEmpty()) {
            var chain = path.stream()
                    .map(r -> r.targetAgentId())
                    .toList();
            sb.append("To escalate unresolved issues, contact: ")
              .append(String.join(" → ", chain)).append(".\n");
        }
    }

    private void appendTeamPeers(StringBuilder sb, OrgRegistry registry,
                                 String agentId, String tenancyId) {
        var units = registry.unitsFor(agentId, tenancyId);
        var peers = units.stream()
                .flatMap(u -> registry.membersOf(u.unitId(), tenancyId).stream())
                .map(m -> m.agentId())
                .filter(id -> !id.equals(agentId))
                .distinct()
                .toList();
        if (!peers.isEmpty()) {
            sb.append("Your team includes: ")
              .append(String.join(", ", peers)).append(".\n");
        }
    }
}

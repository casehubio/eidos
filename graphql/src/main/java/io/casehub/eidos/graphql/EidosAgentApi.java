package io.casehub.eidos.graphql;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentMatch;
import io.casehub.eidos.api.AgentQuery;
import io.casehub.eidos.api.AgentRegistry;
import io.casehub.platform.api.identity.CurrentPrincipal;
import io.casehub.platform.api.mcp.McpDomain;
import io.casehub.platform.api.mcp.PathParam;
import io.casehub.platform.api.mcp.PlatformQuery;
import io.casehub.platform.api.mcp.QueryParam;
import io.casehub.platform.api.mcp.RestPath;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@McpDomain(value = "eidos/agents", app = "eidos",
    basePath = "/api/eidos/agents",
    summary = "Agent identity registry — query agent descriptors with rich personality, capability, and disposition data")
@ApplicationScoped
public class EidosAgentApi {

    private final AgentRegistry registry;
    private final CurrentPrincipal currentPrincipal;

    @Inject
    public EidosAgentApi(AgentRegistry registry, CurrentPrincipal currentPrincipal) {
        this.registry = registry;
        this.currentPrincipal = currentPrincipal;
    }

    @PlatformQuery("Find agents matching optional criteria — no params lists all")
    @RestPath("")
    public List<AgentDescriptor> findAgents(
            @QueryParam("slot") String slot,
            @QueryParam("capability") String capabilityName,
            @QueryParam("goal") String goalName,
            @QueryParam("domain") String taskDomain,
            @QueryParam("maxDepth") Integer maxDepth) {
        String tenancyId = currentPrincipal.tenancyId();
        var query = new AgentQuery(slot, capabilityName, tenancyId, taskDomain, goalName, maxDepth);
        return registry.find(query).stream()
                .map(AgentMatch::descriptor)
                .map(this::filterPrivate)
                .toList();
    }

    @PlatformQuery("Get a specific agent descriptor by ID")
    @RestPath("")
    public AgentDescriptor getAgent(@PathParam("agentId") String agentId) {
        return registry.findById(agentId, currentPrincipal.tenancyId())
                .map(this::filterPrivate)
                .orElse(null);
    }

    private AgentDescriptor filterPrivate(AgentDescriptor d) {
        if (d.goals().isEmpty() && d.constraints().isEmpty()) {
            return d;
        }
        return d.toBuilder()
                .goals(d.publicGoals())
                .constraints(d.publicConstraints())
                .build();
    }
}

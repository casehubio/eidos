package io.casehub.eidos.runtime.resolver;

import io.casehub.eidos.api.*;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import io.casehub.platform.agent.AgentResolver;
import io.casehub.platform.agent.ResolvedAgent;
import io.casehub.platform.api.identity.CurrentPrincipal;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@DefaultBean
@ApplicationScoped
public class EidosAgentResolver implements AgentResolver {

    private final AgentRegistry registry;
    private final SystemPromptRenderer renderer;
    private final CurrentPrincipal currentPrincipal;

    @Inject
    public EidosAgentResolver(AgentRegistry registry,
                              SystemPromptRenderer renderer,
                              CurrentPrincipal currentPrincipal) {
        this.registry = registry;
        this.renderer = renderer;
        this.currentPrincipal = currentPrincipal;
    }

    @Override
    public Optional<ResolvedAgent> resolve(String agentId) {
        return registry.findById(agentId, currentPrincipal.tenancyId())
                .map(this::toResolvedAgent);
    }

    @Override
    public List<io.casehub.platform.agent.AgentDescriptor> discover() {
        return registry.find(AgentQuery.all(currentPrincipal.tenancyId()))
                .stream()
                .map(match -> toDiscoveryDescriptor(match.descriptor()))
                .toList();
    }

    private ResolvedAgent toResolvedAgent(AgentDescriptor descriptor) {
        var prompt = renderer.render(descriptor,
                AgentPromptContext.forFormat(RenderFormat.MARKDOWN));
        return new ResolvedAgent(
                descriptor.agentId(),
                prompt.content(),
                resolveModel(descriptor),
                null,
                buildMetadata(descriptor));
    }

    private String resolveModel(AgentDescriptor descriptor) {
        return descriptor.capabilities().stream()
                .map(AgentCapability::modelRef)
                .filter(ref -> ref != null)
                .findFirst()
                .orElse(null);
    }

    private io.casehub.platform.agent.AgentDescriptor toDiscoveryDescriptor(AgentDescriptor descriptor) {
        return new io.casehub.platform.agent.AgentDescriptor(
                descriptor.agentId(),
                descriptor.name(),
                descriptor.briefing(),
                buildMetadata(descriptor));
    }

    private Map<String, String> buildMetadata(AgentDescriptor descriptor) {
        var meta = new HashMap<String, String>();
        if (descriptor.slot() != null) meta.put("slot", descriptor.slot());
        if (descriptor.domainVocabulary() != null) meta.put("domainVocabulary", descriptor.domainVocabulary());
        if (descriptor.archetype() != null) meta.put("archetype", descriptor.archetype());
        return meta;
    }
}

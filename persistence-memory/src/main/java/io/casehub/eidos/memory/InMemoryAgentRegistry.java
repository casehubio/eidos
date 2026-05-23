package io.casehub.eidos.memory;

import io.casehub.eidos.api.*;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Alternative
@Priority(1)
@ApplicationScoped
public class InMemoryAgentRegistry implements AgentRegistry {

    private final ConcurrentHashMap<String, AgentDescriptor> store = new ConcurrentHashMap<>();

    @Override
    public void register(AgentDescriptor descriptor) {
        store.put(descriptor.agentId(), descriptor);
    }

    @Override
    public Optional<AgentDescriptor> findById(String agentId, String tenancyId) {
        return Optional.ofNullable(store.get(agentId))
            .filter(d -> d.tenancyId().equals(tenancyId));
    }

    @Override
    public List<AgentDescriptor> find(AgentQuery query) {
        return store.values().stream()
            .filter(d -> d.tenancyId().equals(query.tenancyId()))
            .filter(d -> query.slot() == null || query.slot().equals(d.slot()))
            .filter(d -> query.capabilityName() == null
                || d.capabilities().stream().anyMatch(c -> c.name().equals(query.capabilityName())))
            .collect(Collectors.toList());
    }
}

package io.casehub.eidos.memory;

import io.casehub.eidos.api.*;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Alternative
@Priority(1)
@ApplicationScoped
public class InMemoryAgentRegistry implements AgentRegistry {

    private final ConcurrentHashMap<String, AgentDescriptor> store = new ConcurrentHashMap<>();

    @Inject Instance<VocabularyRegistry> vocabularyRegistry;

    @Override
    public void register(AgentDescriptor descriptor) {
        // Validate capability vocabularies before storing (only if VocabularyRegistry available)
        if (vocabularyRegistry.isResolvable()) {
            CapabilityVocabularyValidator.validate(descriptor, vocabularyRegistry.get());
        }
        store.put(descriptor.agentId(), descriptor);
    }

    @Override
    public Optional<AgentDescriptor> findById(String agentId, String tenancyId) {
        Objects.requireNonNull(agentId, "agentId");
        Objects.requireNonNull(tenancyId, "tenancyId");
        return Optional.ofNullable(store.get(agentId))
            .filter(d -> d.tenancyId().equals(tenancyId));
    }

    void clear() {
        store.clear();
    }

    @Override
    public List<AgentDescriptor> find(AgentQuery query) {
        return store.values().stream()
            .filter(d -> d.tenancyId().equals(query.tenancyId()))
            .filter(d -> query.slot() == null || Objects.equals(d.slot(), query.slot()))
            .filter(d -> query.capabilityName() == null
                || d.capabilities().stream().anyMatch(c -> matchesCapability(c, query.capabilityName())))
            .filter(d -> query.taskDomain() == null
                || d.capabilities().stream().noneMatch(c ->
                    c.excludedDomains() != null && c.excludedDomains().contains(query.taskDomain())))
            .collect(Collectors.toList());
    }

    private boolean matchesCapability(AgentCapability capability, String requestedName) {
        // Exact match always works
        if (capability.name().equals(requestedName)) {
            return true;
        }

        // No vocabulary grounding or no registry → exact match only
        if (capability.capabilityVocabulary() == null || !vocabularyRegistry.isResolvable()) {
            return false;
        }

        // Check subsumption via vocabulary
        var registry = vocabularyRegistry.get();
        MatchDegree degree = registry.match(
            capability.capabilityVocabulary(),
            capability.name(),
            requestedName
        );

        return !(degree instanceof MatchDegree.None);
    }
}

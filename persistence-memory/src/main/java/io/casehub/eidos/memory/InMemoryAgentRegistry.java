package io.casehub.eidos.memory;

import io.casehub.eidos.api.*;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.util.Comparator;
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
    public List<AgentMatch> find(AgentQuery query) {
        var stream = store.values().stream()
            .filter(d -> d.tenancyId().equals(query.tenancyId()))
            .filter(d -> query.slot() == null || Objects.equals(d.slot(), query.slot()))
            .filter(d -> query.taskDomain() == null
                || d.capabilities().stream().noneMatch(c ->
                    c.excludedDomains() != null && c.excludedDomains().contains(query.taskDomain())));

        if (query.capabilityName() == null) {
            return stream
                .map(d -> new AgentMatch(d, null))
                .collect(Collectors.toList());
        }

        return stream
            .map(d -> {
                var resolved = resolveCapability(d, query.capabilityName());
                return resolved != null ? new AgentMatch(d, resolved) : null;
            })
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(AgentMatch::resolvedCapability,
                Comparator.comparing(ResolvedCapability::degree)))
            .collect(Collectors.toList());
    }

    private ResolvedCapability resolveCapability(AgentDescriptor descriptor, String capabilityName) {
        if (!vocabularyRegistry.isResolvable()) {
            return descriptor.capabilities().stream()
                .filter(c -> c.name().equals(capabilityName))
                .findFirst()
                .map(c -> new ResolvedCapability(c, new MatchDegree.Exact()))
                .orElse(null);
        }
        return CapabilityResolver.resolve(
            descriptor.capabilities(), capabilityName, vocabularyRegistry.get());
    }
}

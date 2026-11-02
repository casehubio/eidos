package io.casehub.eidos.org.memory;

import io.casehub.eidos.org.api.*;
import io.casehub.eidos.org.api.spi.GroupInstanceStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.annotation.Priority;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Alternative
@Priority(1)
@ApplicationScoped
public class InMemoryGroupInstanceStore implements GroupInstanceStore {

    private final ConcurrentHashMap<String, GroupInstance> store = new ConcurrentHashMap<>();

    @Override
    public void save(GroupInstance instance) {
        store.compute(instance.instanceId(), (id, existing) -> {
            if (existing != null) {
                if (existing.version() != instance.version())
                    throw new StaleGroupInstanceException(id, instance.version());
                if (!existing.tenancyId().equals(instance.tenancyId()))
                    throw new OrgValidationException("tenancyId", "cannot change tenancyId on existing instance");
            }
            return new GroupInstance(
                instance.instanceId(), instance.specId(), instance.tenancyId(),
                instance.state(), instance.formationOrigin(),
                instance.topology(), instance.topologyVocabulary(),
                instance.effectiveAuthority(),
                instance.members(), instance.roleAdoptions(), instance.missionCommitments(),
                instance.createdAt(), instance.lastTransitionAt(),
                (existing != null ? existing.version() : instance.version()) + 1
            );
        });
    }

    @Override
    public Optional<GroupInstance> findById(String instanceId, String tenancyId) {
        var found = store.get(instanceId);
        if (found != null && found.tenancyId().equals(tenancyId)) return Optional.of(found);
        return Optional.empty();
    }

    @Override
    public List<GroupInstance> find(GroupInstanceQuery query) {
        return store.values().stream()
            .filter(gi -> gi.tenancyId().equals(query.tenancyId()))
            .filter(gi -> query.specId() == null || gi.specId().equals(query.specId()))
            .filter(gi -> query.state() == null || gi.state() == query.state())
            .filter(gi -> query.agentId() == null ||
                gi.members().stream().anyMatch(m -> m.agentId().equals(query.agentId())))
            .toList();
    }

    @Override
    public void remove(String instanceId, String tenancyId) {
        store.computeIfPresent(instanceId, (id, existing) -> {
            if (!existing.tenancyId().equals(tenancyId)) return existing;
            return null;
        });
    }
}

package io.casehub.eidos.runtime.registry.jpa;

import io.casehub.eidos.api.*;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.panache.common.Parameters;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@IfBuildProperty(name = "casehub.eidos.reactive.enabled", stringValue = "true")
@ApplicationScoped
public class JpaReactiveAgentRegistry implements ReactiveAgentRegistry {

    @Inject AgentDescriptorReactivePanacheRepo repo;
    @Inject AgentDescriptorMapper mapper;

    @Override
    @WithTransaction
    public Uni<Void> register(AgentDescriptor descriptor) {
        return repo.delete("agentId", descriptor.agentId())
                   .chain(() -> repo.persist(mapper.toEntity(descriptor)))
                   .replaceWithVoid();
    }

    @Override
    @WithSession
    public Uni<Optional<AgentDescriptor>> findById(String agentId, String tenancyId) {
        Objects.requireNonNull(tenancyId, "tenancyId");
        return repo.find(
                "SELECT DISTINCT a FROM AgentDescriptorEntity a"
                + " LEFT JOIN FETCH a.capabilities"
                + " WHERE a.agentId = ?1 AND a.tenancyId = ?2", agentId, tenancyId)
            .firstResult()
            .map(e -> Optional.ofNullable(e).map(mapper::toRecord));
    }

    @Override
    @WithSession
    public Uni<List<AgentDescriptor>> find(AgentQuery query) {
        String fetchJoin = query.capabilityName() != null
            ? "JOIN FETCH a.capabilities c"
            : "LEFT JOIN FETCH a.capabilities c";

        var jpql = new StringBuilder(
            "SELECT DISTINCT a FROM AgentDescriptorEntity a " + fetchJoin
            + " WHERE a.tenancyId = :tenancyId");

        var params = Parameters.with("tenancyId", query.tenancyId());
        if (query.slot() != null) {
            jpql.append(" AND a.slot = :slot");
            params.and("slot", query.slot());
        }
        if (query.capabilityName() != null) {
            jpql.append(" AND c.name = :capabilityName");
            params.and("capabilityName", query.capabilityName());
        }

        return repo.list(jpql.toString(), params)
                   .map(list -> list.stream().map(mapper::toRecord).toList());
    }
}

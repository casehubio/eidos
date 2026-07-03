package io.casehub.eidos.runtime.registry.jpa;

import io.casehub.eidos.api.*;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.panache.common.Parameters;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.jboss.logging.Logger;

@IfBuildProperty(name = "casehub.eidos.reactive.enabled", stringValue = "true")
@ApplicationScoped
public class JpaReactiveAgentRegistry implements ReactiveAgentRegistry {

    private static final Logger LOG = Logger.getLogger(JpaReactiveAgentRegistry.class);

    @Inject AgentDescriptorReactivePanacheRepo repo;
    @Inject AgentDescriptorMapper mapper;
    @Inject VocabularyRegistry vocabularyRegistry;

    @Override
    @WithTransaction
    public Uni<Void> register(AgentDescriptor descriptor) {
        CapabilityVocabularyValidator.validate(descriptor, vocabularyRegistry);
        return repo.delete("agentId = ?1 AND tenancyId = ?2",
                          descriptor.agentId(), descriptor.tenancyId())
                   .chain(() -> repo.persist(mapper.toEntity(descriptor)))
                   .replaceWithVoid();
    }

    @Override
    @WithSession
    public Uni<Optional<AgentDescriptor>> findById(String agentId, String tenancyId) {
        Objects.requireNonNull(agentId, "agentId");
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
    public Uni<List<AgentMatch>> find(AgentQuery query) {
        String fetchJoin = (query.capabilityName() != null || query.taskDomain() != null)
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

        // Capability matching with vocabulary expansion
        if (query.capabilityName() != null) {
            Map<String, Set<String>> expansion = vocabularyRegistry.expandForMatchingByVocabulary(query.capabilityName());
            int totalExpanded = expansion.values().stream().mapToInt(Set::size).sum();
            if (totalExpanded > JpaAgentRegistry.MAX_EXPANSION_SIZE) {
                LOG.warnf("Vocabulary expansion for '%s' produced %d terms across %d vocabularies;"
                    + " query may be slow", query.capabilityName(), totalExpanded, expansion.size());
            }
            if (expansion.isEmpty()) {
                // No vocabulary grounding - exact match only
                jpql.append(" AND c.name = :capabilityName");
                params.and("capabilityName", query.capabilityName());
            } else {
                // Vocabulary grounding - expand to include specialized terms
                jpql.append(" AND (c.name = :capabilityName");
                params.and("capabilityName", query.capabilityName());
                int idx = 0;
                for (var entry : expansion.entrySet()) {
                    jpql.append(" OR (c.capabilityVocabulary = :vocab").append(idx)
                        .append(" AND c.name IN :expanded").append(idx).append(")");
                    params.and("vocab" + idx, entry.getKey());
                    params.and("expanded" + idx, entry.getValue());
                    idx++;
                }
                jpql.append(")");
            }
        }

        if (query.taskDomain() != null) {
            jpql.append(" AND :taskDomain NOT MEMBER OF c.excludedDomains");
            params.and("taskDomain", query.taskDomain());
        }

        return repo.list(jpql.toString(), params)
                   .map(list -> {
                       var descriptors = list.stream().map(mapper::toRecord).toList();
                       if (query.capabilityName() == null) {
                           return descriptors.stream()
                               .map(d -> new AgentMatch(d, null))
                               .toList();
                       }
                       return descriptors.stream()
                           .map(d -> {
                               var resolved = CapabilityResolver.resolve(
                                   d.capabilities(), query.capabilityName(), vocabularyRegistry);
                               return new AgentMatch(d, resolved);
                           })
                           .sorted(Comparator.comparing(AgentMatch::resolvedCapability,
                               Comparator.nullsLast(Comparator.comparing(ResolvedCapability::degree))))
                           .toList();
                   });
    }
}

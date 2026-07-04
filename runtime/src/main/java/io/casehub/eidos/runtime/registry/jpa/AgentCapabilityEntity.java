package io.casehub.eidos.runtime.registry.jpa;

import jakarta.persistence.*;
import java.util.Set;

/**
 * JPA entity for {@link io.casehub.eidos.api.AgentCapability}.
 *
 * <p>The class is {@code public} because Hibernate Reactive bytecode enhancement requires it.
 * All fields are package-private intentionally: Hibernate enhancement accesses fields directly
 * via bytecode instrumentation — no getters or setters are needed or generated.
 */
@Entity
@Table(name = "agent_capability")
public class AgentCapabilityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "descriptor_id", nullable = false)
    AgentDescriptorEntity descriptor;

    @Column(name = "agent_id")   String agentId;
    @Column(name = "tenancy_id") String tenancyId;

    @Column(nullable = false)
    String name;

    @Column(columnDefinition = "TEXT") String description;

    @Column(name = "capability_vocabulary") String capabilityVocabulary;

    @Column(name = "quality_hint")        Double qualityHint;
    @Column(name = "latency_hint_p50_ms") Long latencyHintP50Ms;
    @Column(name = "cost_hint")           String costHint;

    @Column(name = "input_types",       columnDefinition = "TEXT") String inputTypes;
    @Column(name = "output_types",      columnDefinition = "TEXT") String outputTypes;
    @Column(columnDefinition = "TEXT")                              String tags;
    @Column(name = "epistemic_domains", columnDefinition = "TEXT") String epistemicDomains;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "agent_capability_excluded_domain",
                     joinColumns = @JoinColumn(name = "capability_id"))
    @Column(name = "domain")
    Set<String> excludedDomains;
}

package io.casehub.eidos.runtime.registry.jpa;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.Set;

@Entity
@Table(name = "agent_capability",
       uniqueConstraints = @UniqueConstraint(columnNames = {"descriptor_id", "name"}))
public class AgentCapabilityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "descriptor_id", nullable = false)
    AgentDescriptorEntity descriptor;

    @Column(name = "agent_id")
    String agentId;
    @Column(name = "tenancy_id")
    String tenancyId;

    @Column(nullable = false)
    String name;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(name = "capability_vocabulary")
    String capabilityVocabulary;

    @Column(name = "quality_hint")
    Double qualityHint;
    @Column(name = "latency_hint_p50_ms")
    Long   latencyHintP50Ms;
    @Column(name = "cost_hint")
    String costHint;

    @Column(name = "input_types", columnDefinition = "TEXT")
    String inputTypes;
    @Column(name = "output_types", columnDefinition = "TEXT")
    String outputTypes;
    @Column(columnDefinition = "TEXT")
    String tags;
    @Column(name = "epistemic_domains", columnDefinition = "TEXT")
    String epistemicDomains;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "agent_capability_excluded_domain",
                     joinColumns = @JoinColumn(name = "capability_id"))
    @Column(name = "domain")
    Set<String> excludedDomains;
}

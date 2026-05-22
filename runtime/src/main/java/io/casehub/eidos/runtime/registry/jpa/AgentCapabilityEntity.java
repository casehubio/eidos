package io.casehub.eidos.runtime.registry.jpa;

import jakarta.persistence.*;

@Entity
@Table(name = "agent_capability")
class AgentCapabilityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    AgentDescriptorEntity descriptor;

    @Column(nullable = false)
    String name;

    @Column(name = "quality_hint")
    double qualityHint;

    @Column(name = "latency_hint_p50_ms")
    Long latencyHintP50Ms;

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
}

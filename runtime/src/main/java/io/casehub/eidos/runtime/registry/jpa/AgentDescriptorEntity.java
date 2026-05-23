package io.casehub.eidos.runtime.registry.jpa;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "agent_descriptor")
public class AgentDescriptorEntity {

    @Id
    @Column(name = "agent_id")
    String agentId;

    @Column(name = "tenancy_id", nullable = false)
    String tenancyId;

    String name;
    String version;
    String provider;

    @Column(name = "model_family")
    String modelFamily;

    @Column(name = "model_version")
    String modelVersion;

    @Column(name = "weights_fingerprint")
    String weightsFingerprint;

    @Column(name = "domain_vocabulary")
    String domainVocabulary;

    @Column(name = "slot_vocabulary")
    String slotVocabulary;

    @Column(name = "disposition_vocabulary")
    String dispositionVocabulary;

    String slot;
    String jurisdiction;

    @Column(name = "data_handling_policy", columnDefinition = "TEXT")
    String dataHandlingPolicy;

    @Column(columnDefinition = "TEXT")
    String disposition;

    @OneToMany(mappedBy = "descriptor", cascade = CascadeType.ALL,
               fetch = FetchType.LAZY, orphanRemoval = true)
    List<AgentCapabilityEntity> capabilities = new ArrayList<>();
}

package io.casehub.eidos.runtime.registry.jpa;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity for {@link io.casehub.eidos.api.AgentDescriptor}.
 *
 * <p>The class is {@code public} because Hibernate Reactive bytecode enhancement requires it.
 * All fields are package-private intentionally: Hibernate enhancement accesses fields directly
 * via bytecode instrumentation — no getters or setters are needed or generated.
 */
@Entity
@Table(name = "agent_descriptor",
       uniqueConstraints = @UniqueConstraint(columnNames = {"agent_id", "tenancy_id"}))
public class AgentDescriptorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "internal_id")
    Long internalId;

    @Column(name = "agent_id", nullable = false)
    String agentId;

    @Column(name = "tenancy_id", nullable = false)
    String tenancyId;

    String name;
    String version;
    String provider;

    @Column(name = "model_family")           String modelFamily;
    @Column(name = "model_version")          String modelVersion;
    @Column(name = "weights_fingerprint")    String weightsFingerprint;
    @Column(name = "domain_vocabulary")      String domainVocabulary;
    @Column(name = "slot_vocabulary")        String slotVocabulary;
    @Column(name = "disposition_vocabulary") String dispositionVocabulary;

    @Column(name = "axis_vocabularies", columnDefinition = "TEXT")
    String axisVocabularies;

    String slot;
    String jurisdiction;

    @Column(name = "data_handling_policy", columnDefinition = "TEXT")
    String dataHandlingPolicy;

    @Column(name = "briefing", columnDefinition = "TEXT")
    String briefing;
    @Column(columnDefinition = "TEXT")
    String templates;


    @Column(columnDefinition = "TEXT")
    String disposition;

    @OneToMany(mappedBy = "descriptor", cascade = CascadeType.ALL,
               fetch = FetchType.LAZY, orphanRemoval = true)
    List<AgentCapabilityEntity> capabilities = new ArrayList<>();

    @OneToMany(mappedBy = "descriptor", cascade = CascadeType.ALL,
               fetch = FetchType.LAZY, orphanRemoval = true)
    List<AgentGoalEntity> goals = new ArrayList<>();

    @OneToMany(mappedBy = "descriptor", cascade = CascadeType.ALL,
               fetch = FetchType.LAZY, orphanRemoval = true)
    List<AgentConstraintEntity> constraints = new ArrayList<>();
}

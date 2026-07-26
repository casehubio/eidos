package io.casehub.eidos.runtime.registry.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "agent_constraint",
       uniqueConstraints = @UniqueConstraint(columnNames = {"descriptor_id", "name"}))
public class AgentConstraintEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "descriptor_id", nullable = false)
    AgentDescriptorEntity descriptor;

    @Column(name = "agent_id")   String agentId;
    @Column(name = "tenancy_id") String tenancyId;

    @Column(nullable = false) String name;
    @Column(columnDefinition = "TEXT", nullable = false) String description;
    @Column(nullable = false) String visibility;
    @Column(nullable = false)
                              String severity;

}

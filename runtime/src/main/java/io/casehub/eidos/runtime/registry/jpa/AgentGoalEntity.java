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
@Table(name = "agent_goal",
       uniqueConstraints = @UniqueConstraint(columnNames = {"descriptor_id", "name"}))
public class AgentGoalEntity {

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
    @Column(nullable = false) String priority;
    @Column(nullable = false) String visibility;
    @Column(name = "capabilities") String capabilities;
    @Column(name = "attributes")
                                   String attributes;

}

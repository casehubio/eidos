package io.casehub.eidos.runtime.graph;

import io.casehub.eidos.api.*;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;

@DefaultBean
@ApplicationScoped
public class NoOpAgentGraphStore implements AgentGraphStore {
    @Override public void recordTask(final AgentTask task) {}
    @Override public void recordOutcome(final AgentTaskId id, final AgentOutcome outcome) {}
    @Override public void linkAttestation(final AgentTaskId id, final AttestationRef ref) {}
}

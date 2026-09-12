package io.casehub.eidos.core.graph;

import io.casehub.eidos.api.*;



public class NoOpAgentGraphStore implements AgentGraphStore {
    @Override public void recordTask(final AgentTask task) {}
    @Override public void recordOutcome(final AgentTaskId id, final AgentOutcome outcome) {}
    @Override public void linkAttestation(final AgentTaskId id, final AttestationRef ref) {}
}

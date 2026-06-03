package io.casehub.eidos.api;

public interface AgentGraphStore {
    void recordTask(AgentTask task);
    void recordOutcome(AgentTaskId id, AgentOutcome outcome);
    void linkAttestation(AgentTaskId id, AttestationRef ref);
}

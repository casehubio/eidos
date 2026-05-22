package io.casehub.eidos.api;

import java.util.List;
import java.util.Optional;

public interface AgentRegistry {
    void register(AgentDescriptor descriptor);
    Optional<AgentDescriptor> findById(String agentId);
    List<AgentDescriptor> findBySlot(String slot);
    List<AgentDescriptor> findByCapability(String capabilityName);
    List<AgentDescriptor> findBySlotAndCapability(String slot, String capabilityName);
}

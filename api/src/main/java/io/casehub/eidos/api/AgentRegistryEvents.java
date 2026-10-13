package io.casehub.eidos.api;

public interface AgentRegistryEvents {
    default void registered(AgentDescriptorRegistered event) {}
    default void updated(AgentDescriptorUpdated event) {}
    default void removed(AgentDescriptorRemoved event) {}
}

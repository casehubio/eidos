package io.casehub.eidos.memory;

import io.casehub.eidos.api.AgentDescriptorRegistered;
import io.casehub.eidos.api.AgentDescriptorRemoved;
import io.casehub.eidos.api.AgentDescriptorUpdated;
import io.casehub.eidos.api.AgentRegistryEvents;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class TestAgentRegistryEvents implements AgentRegistryEvents {

    final List<AgentDescriptorRegistered> registered = new ArrayList<>();
    final List<AgentDescriptorUpdated> updated = new ArrayList<>();
    final List<AgentDescriptorRemoved> removed = new ArrayList<>();

    @Override
    public void registered(AgentDescriptorRegistered event) {
        registered.add(event);
    }

    @Override
    public void updated(AgentDescriptorUpdated event) {
        updated.add(event);
    }

    @Override
    public void removed(AgentDescriptorRemoved event) {
        removed.add(event);
    }

    void clear() {
        registered.clear();
        updated.clear();
        removed.clear();
    }
}

package io.casehub.eidos.runtime.registry;

import io.casehub.eidos.api.AgentDescriptorRegistered;
import io.casehub.eidos.api.AgentDescriptorRemoved;
import io.casehub.eidos.api.AgentDescriptorUpdated;
import io.casehub.eidos.api.AgentRegistryEvents;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

@DefaultBean
@ApplicationScoped
public class CdiAgentRegistryEvents implements AgentRegistryEvents {

    @Inject Event<AgentDescriptorRegistered> registeredEvent;
    @Inject Event<AgentDescriptorUpdated> updatedEvent;
    @Inject Event<AgentDescriptorRemoved> removedEvent;

    @Override
    public void registered(AgentDescriptorRegistered event) {
        registeredEvent.fire(event);
    }

    @Override
    public void updated(AgentDescriptorUpdated event) {
        updatedEvent.fire(event);
    }

    @Override
    public void removed(AgentDescriptorRemoved event) {
        removedEvent.fire(event);
    }
}

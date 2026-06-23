package io.casehub.eidos.api.spi;

import io.casehub.eidos.api.AgentDescriptor;

import java.util.List;

@FunctionalInterface
public interface AgentDescriptorRegistrar {
    List<AgentDescriptor> descriptors();
}

package io.casehub.eidos.runtime.registrar;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentRegistry;
import io.casehub.eidos.api.spi.AgentDescriptorRegistrar;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@IfBuildProperty(name = "casehub.eidos.reactive.enabled",
                 stringValue = "false", enableIfMissing = true)
@ApplicationScoped
public class AgentDescriptorBootstrap {

    @Inject AgentRegistry registry;
    @Inject @Any Instance<AgentDescriptorRegistrar> registrars;

    void onStartup(@Observes StartupEvent ev) {
        registerAll(registrars, registry);
    }

    static void registerAll(Iterable<AgentDescriptorRegistrar> registrars,
                            AgentRegistry registry) {
        final var all = new ArrayList<AgentDescriptor>();
        registrars.forEach(r -> all.addAll(r.descriptors()));

        final var seen = new HashSet<String>();
        for (final var d : all) {
            final var key = d.agentId() + "\0" + d.tenancyId();
            if (!seen.add(key)) {
                throw new IllegalStateException(
                    "Duplicate descriptor: agentId=" + d.agentId()
                    + ", tenancyId=" + d.tenancyId());
            }
        }

        all.forEach(registry::register);
    }
}

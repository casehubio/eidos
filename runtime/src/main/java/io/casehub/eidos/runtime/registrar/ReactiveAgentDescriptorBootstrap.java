package io.casehub.eidos.runtime.registrar;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.ReactiveAgentRegistry;
import io.casehub.eidos.api.spi.AgentDescriptorRegistrar;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.List;

@IfBuildProperty(name = "casehub.eidos.reactive.enabled", stringValue = "true")
@ApplicationScoped
public class ReactiveAgentDescriptorBootstrap {

    @Inject ReactiveAgentRegistry registry;
    @Inject @Any Instance<AgentDescriptorRegistrar> registrars;

    void onStartup(@Observes StartupEvent ev) {
        registerAll(registrars, registry).await().indefinitely();
    }

    static Uni<Void> registerAll(Iterable<AgentDescriptorRegistrar> registrars,
                                  ReactiveAgentRegistry registry) {
        List<AgentDescriptor> validated = DescriptorCollector.collectAndValidate(registrars);
        Uni<Void> chain = Uni.createFrom().voidItem();
        for (AgentDescriptor d : validated) {
            chain = chain.chain(() -> registry.register(d));
        }
        return chain;
    }
}

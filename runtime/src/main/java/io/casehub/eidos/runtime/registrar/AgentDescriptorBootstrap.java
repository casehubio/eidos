package io.casehub.eidos.runtime.registrar;

import io.casehub.eidos.api.AgentRegistry;
import io.casehub.eidos.api.TemplateRegistry;
import io.casehub.eidos.api.VocabularyRegistry;
import io.casehub.eidos.api.spi.AgentDescriptorRegistrar;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@IfBuildProperty(name = "casehub.eidos.reactive.enabled",
                 stringValue = "false", enableIfMissing = true)
@ApplicationScoped
public class AgentDescriptorBootstrap {

    @Inject
    AgentRegistry                      registry;
    @Inject
    @Any
    Instance<AgentDescriptorRegistrar> registrars;
    @Inject
    TemplateRegistry                   templateRegistry;
    @Inject
    VocabularyRegistry                 vocabRegistry;

    static void registerAll(Iterable<AgentDescriptorRegistrar> registrars,
                            AgentRegistry registry, TemplateRegistry templateRegistry,
                            VocabularyRegistry vocabRegistry) {
        DescriptorCollector.collectAndValidate(registrars, templateRegistry, vocabRegistry)
                           .forEach(registry::register);
    }

    void onStartup(@Observes StartupEvent ev) {
        registerAll(registrars, registry, templateRegistry, vocabRegistry);
    }
}

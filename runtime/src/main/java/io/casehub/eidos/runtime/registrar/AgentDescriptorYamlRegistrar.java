package io.casehub.eidos.runtime.registrar;

import io.casehub.eidos.api.AgentRegistry;
import io.casehub.eidos.api.VocabularyRegistry;
import io.casehub.eidos.core.registrar.ClasspathYamlDescriptorRegistrar;
import io.casehub.platform.bootstrap.spi.ClasspathYamlRegistrar;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.InputStream;
import java.util.Set;

@ApplicationScoped
public class AgentDescriptorYamlRegistrar implements ClasspathYamlRegistrar {

    private final AgentRegistry registry;
    private final VocabularyRegistry vocabRegistry;
    private final ClasspathYamlDescriptorRegistrar descriptorParser = new ClasspathYamlDescriptorRegistrar();

    @Inject
    public AgentDescriptorYamlRegistrar(AgentRegistry registry, VocabularyRegistry vocabRegistry) {
        this.registry = registry;
        this.vocabRegistry = vocabRegistry;
    }

    @Override
    public String sectionKey() {
        return "agents";
    }

    @Override
    public Set<String> classpathPaths() {
        return Set.of("agents");
    }

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public void register(String resourcePath, InputStream content) {
        try {
            var descriptors = descriptorParser.loadFrom(content, vocabRegistry);
            descriptors.stream()
                    .map(ArchetypeDeriver::deriveArchetype)
                    .forEach(registry::register);
        } catch (IllegalStateException e) {
            if (e.getMessage() != null && e.getMessage().contains("end-of-input")) {
                return;
            }
            throw e;
        }
    }
}

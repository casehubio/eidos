package io.casehub.eidos.runtime.registrar;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.VocabularyRegistry;
import io.casehub.eidos.api.spi.AgentDescriptorRegistrar;
import io.casehub.eidos.runtime.yaml.EidosDescriptorModule;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

@ApplicationScoped
public class ClasspathYamlDescriptorRegistrar implements AgentDescriptorRegistrar {

    private static final String RESOURCE_PATH = "META-INF/eidos/descriptors.yaml";

    @Override
    public List<AgentDescriptor> descriptors() {
        final Enumeration<URL> urls;
        try {
            urls = Thread.currentThread().getContextClassLoader().getResources(RESOURCE_PATH);
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to scan classpath for " + RESOURCE_PATH, e);
        }

        final var all = new ArrayList<AgentDescriptor>();
        while (urls.hasMoreElements()) {
            final var url = urls.nextElement();
            try (final var stream = url.openStream()) {
                all.addAll(loadFrom(stream));
            } catch (final Exception e) {
                throw new IllegalStateException(
                        "Failed to load descriptors from " + url + ": " + e.getMessage(), e);
            }
        }
        return List.copyOf(all);
    }

    List<AgentDescriptor> loadFrom(final InputStream yaml) {
        return loadFrom(yaml, null);
    }

    public List<AgentDescriptor> loadFrom(final InputStream yaml, final VocabularyRegistry vocabRegistry) {
        if (yaml == null) return List.of();
        try {
            var mapper = EidosDescriptorModule.createMapper(vocabRegistry);
            var file = mapper.readValue(yaml, DescriptorFile.class);
            if (file.descriptors == null || file.descriptors.isEmpty()) return List.of();
            return List.copyOf(file.descriptors);
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to parse YAML: " + e.getMessage(), e);
        }
    }

    static class DescriptorFile {
        public List<AgentDescriptor> descriptors;
    }
}

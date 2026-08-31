package io.casehub.eidos.runtime.registrar;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.VocabularyRegistry;
import io.casehub.eidos.api.spi.AgentDescriptorRegistrar;
import io.casehub.eidos.runtime.yaml.DescriptorPreprocessor;
import io.casehub.eidos.runtime.yaml.EidosDescriptorModule;
import io.casehub.yaml.core.resolver.VariableSource;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    @SuppressWarnings("unchecked")
    public List<AgentDescriptor> loadFrom(final InputStream yaml, final VocabularyRegistry vocabRegistry) {
        if (yaml == null) return List.of();
        try {
            var plainMapper = new ObjectMapper(new YAMLFactory());
            var rawMap = (Map<String, Object>) plainMapper.readValue(yaml, Map.class);
            if (rawMap == null) return List.of();

            var externalSources = new LinkedHashMap<String, VariableSource>();
            try {
                var config = org.eclipse.microprofile.config.ConfigProvider.getConfig();
                externalSources.put("config", name ->
                        config.getOptionalValue(name, String.class).orElse(null));
            } catch (Exception ignored) {}

            var descriptorMaps = DescriptorPreprocessor.preprocess(
                    rawMap, externalSources,
                    Thread.currentThread().getContextClassLoader());

            if (descriptorMaps.isEmpty()) return List.of();

            var eidosMapper = EidosDescriptorModule.createMapper(vocabRegistry);
            var result = new ArrayList<AgentDescriptor>();
            for (var map : descriptorMaps) {
                try {
                    var node = eidosMapper.valueToTree(map);
                    result.add(eidosMapper.treeToValue(node, AgentDescriptor.class));
                } catch (Exception e) {
                    throw new IllegalStateException(
                            "Failed to deserialize descriptor '"
                            + map.get("agentId") + "': " + e.getMessage(), e);
                }
            }
            return List.copyOf(result);
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to parse YAML: " + e.getMessage(), e);
        }
    }
}

package io.casehub.eidos.runtime.template;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.casehub.eidos.api.DescriptorTemplate;
import io.casehub.eidos.api.spi.TemplateRegistrar;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

@ApplicationScoped
public class ClasspathYamlTemplateRegistrar implements TemplateRegistrar {

    private static final String RESOURCE_PATH = "META-INF/eidos/templates.yaml";
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

    @Override
    public List<DescriptorTemplate> templates() {
        final Enumeration<URL> urls;
        try {
            urls = Thread.currentThread().getContextClassLoader().getResources(RESOURCE_PATH);
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to scan classpath for " + RESOURCE_PATH, e);
        }

        final var all = new ArrayList<DescriptorTemplate>();
        while (urls.hasMoreElements()) {
            final var url = urls.nextElement();
            try (final var stream = url.openStream()) {
                all.addAll(loadFrom(stream));
            } catch (final Exception e) {
                throw new IllegalStateException(
                    "Failed to load templates from " + url + ": " + e.getMessage(), e);
            }
        }
        return List.copyOf(all);
    }

    List<DescriptorTemplate> loadFrom(final InputStream yaml) {
        if (yaml == null) return List.of();
        final TemplateFile file;
        try {
            file = YAML_MAPPER.readValue(yaml, TemplateFile.class);
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to parse templates YAML: " + e.getMessage(), e);
        }
        if (file.templates == null || file.templates.isEmpty()) return List.of();

        final var result = new ArrayList<DescriptorTemplate>(file.templates.size());
        for (final var cfg : file.templates) {
            result.add(new DescriptorTemplate(cfg.id, cfg.name, cfg.parameters, cfg.content));
        }
        return result;
    }

    static class TemplateFile {
        public List<TemplateConfig> templates;
    }

    static class TemplateConfig {
        public String id, name, content;
        public List<String> parameters;
    }
}

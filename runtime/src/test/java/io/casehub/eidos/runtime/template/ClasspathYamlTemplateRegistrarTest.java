package io.casehub.eidos.runtime.template;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.*;

class ClasspathYamlTemplateRegistrarTest {

    @Test void loads_static_template_from_yaml() {
        var yaml = """
            templates:
              - id: style
                name: Style Guide
                content: "Follow these conventions."
            """;
        var registrar = new ClasspathYamlTemplateRegistrar();
        var templates = registrar.loadFrom(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
        assertThat(templates).hasSize(1);
        assertThat(templates.get(0).id()).isEqualTo("style");
        assertThat(templates.get(0).parameters()).isEmpty();
        assertThat(templates.get(0).content()).isEqualTo("Follow these conventions.");
    }

    @Test void loads_parameterized_template_from_yaml() {
        var yaml = """
            templates:
              - id: villain
                name: Villain
                parameters: [catchphrase, nemesis]
                content: "Catchphrase: ${catchphrase}. Nemesis: ${nemesis}."
            """;
        var registrar = new ClasspathYamlTemplateRegistrar();
        var templates = registrar.loadFrom(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
        assertThat(templates).hasSize(1);
        assertThat(templates.get(0).parameters()).containsExactly("catchphrase", "nemesis");
    }

    @Test void empty_file_returns_empty_list() {
        var yaml = "templates:\n";
        var registrar = new ClasspathYamlTemplateRegistrar();
        var templates = registrar.loadFrom(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
        assertThat(templates).isEmpty();
    }

    @Test void multiple_templates_in_one_file() {
        var yaml = """
            templates:
              - id: a
                name: A
                content: "content a"
              - id: b
                name: B
                content: "content b"
            """;
        var registrar = new ClasspathYamlTemplateRegistrar();
        var templates = registrar.loadFrom(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
        assertThat(templates).hasSize(2);
        assertThat(templates).extracting("id").containsExactly("a", "b");
    }

    @Test void multiline_content_preserved() {
        var yaml = """
            templates:
              - id: multi
                name: Multi
                content: |
                  Line one.
                  Line two.
                  Line three.
            """;
        var registrar = new ClasspathYamlTemplateRegistrar();
        var templates = registrar.loadFrom(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
        assertThat(templates.get(0).content()).contains("Line one.\nLine two.\nLine three.");
    }
}

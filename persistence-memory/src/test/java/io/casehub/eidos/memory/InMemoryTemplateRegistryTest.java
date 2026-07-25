package io.casehub.eidos.memory;

import io.casehub.eidos.api.DescriptorTemplate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class InMemoryTemplateRegistryTest {

    @Test void register_and_resolve() {
        var registry = new InMemoryTemplateRegistry();
        var t = new DescriptorTemplate("style", "Style", List.of(), "content");
        registry.register(t);
        assertThat(registry.resolve("style")).contains(t);
    }

    @Test void resolve_unknown_returns_empty() {
        var registry = new InMemoryTemplateRegistry();
        assertThat(registry.resolve("nope")).isEmpty();
    }

    @Test void all_returns_registered_templates() {
        var registry = new InMemoryTemplateRegistry();
        var t1 = new DescriptorTemplate("a", "A", List.of(), "content a");
        var t2 = new DescriptorTemplate("b", "B", List.of(), "content b");
        registry.register(t1);
        registry.register(t2);
        assertThat(registry.all()).containsExactlyInAnyOrder(t1, t2);
    }

    @Test void duplicate_id_throws() {
        var registry = new InMemoryTemplateRegistry();
        registry.register(new DescriptorTemplate("dup", "A", List.of(), "a"));
        assertThatThrownBy(() -> registry.register(new DescriptorTemplate("dup", "B", List.of(), "b")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("dup");
    }

    @Test void placeholder_validation_rejects_undeclared_parameter() {
        var registry = new InMemoryTemplateRegistry();
        var t = new DescriptorTemplate("bad", "Bad", List.of("name"),
            "Hello ${name}, your nemesis is ${nemesis}.");
        assertThatThrownBy(() -> registry.register(t))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("nemesis");
    }

    @Test void placeholder_validation_accepts_matching_params() {
        var registry = new InMemoryTemplateRegistry();
        var t = new DescriptorTemplate("ok", "OK", List.of("name", "nemesis"),
            "Hello ${name}, your nemesis is ${nemesis}.");
        assertThatNoException().isThrownBy(() -> registry.register(t));
    }

    @Test void static_template_with_no_placeholders_passes() {
        var registry = new InMemoryTemplateRegistry();
        var t = new DescriptorTemplate("plain", "Plain", List.of(), "No variables here.");
        assertThatNoException().isThrownBy(() -> registry.register(t));
    }

    @Test void clear_removes_all() {
        var registry = new InMemoryTemplateRegistry();
        registry.register(new DescriptorTemplate("x", "X", List.of(), "content"));
        registry.clear();
        assertThat(registry.resolve("x")).isEmpty();
        assertThat(registry.all()).isEmpty();
    }
}

package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class DescriptorTemplateTest {

    @Test void valid_static_template() {
        var t = new DescriptorTemplate("style-guide", "Style Guide", List.of(), "You follow these conventions.");
        assertThat(t.id()).isEqualTo("style-guide");
        assertThat(t.name()).isEqualTo("Style Guide");
        assertThat(t.parameters()).isEmpty();
        assertThat(t.content()).isEqualTo("You follow these conventions.");
    }

    @Test void valid_parameterized_template() {
        var t = new DescriptorTemplate("villain", "Villain", List.of("catchphrase", "nemesis"),
            "Your catchphrase is \"${catchphrase}\". Your nemesis is ${nemesis}.");
        assertThat(t.parameters()).containsExactly("catchphrase", "nemesis");
    }

    @Test void null_parameters_defaulted_to_empty() {
        var t = new DescriptorTemplate("x", "X", null, "content");
        assertThat(t.parameters()).isEmpty();
    }

    @Test void parameters_are_immutable() {
        var params = new java.util.ArrayList<>(List.of("a", "b"));
        var t = new DescriptorTemplate("x", "X", params, "content");
        assertThatThrownBy(() -> t.parameters().add("c")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test void null_id_throws() {
        assertThatThrownBy(() -> new DescriptorTemplate(null, "X", List.of(), "content"))
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName()).isEqualTo("template.id"));
    }

    @Test void blank_id_throws() {
        assertThatThrownBy(() -> new DescriptorTemplate("  ", "X", List.of(), "content"))
            .isInstanceOf(AgentValidationException.class);
    }

    @Test void null_content_throws() {
        assertThatThrownBy(() -> new DescriptorTemplate("x", "X", List.of(), null))
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName()).isEqualTo("template.content"));
    }

    @Test void content_allows_newlines() {
        assertThatNoException().isThrownBy(() ->
            new DescriptorTemplate("x", "X", List.of(), "line one\nline two\nline three"));
    }

    @Test void content_rejects_over_4000_chars() {
        assertThatThrownBy(() -> new DescriptorTemplate("x", "X", List.of(), "x".repeat(4001)))
            .isInstanceOf(AgentValidationException.class);
    }

    @Test void id_rejects_over_100_chars() {
        assertThatThrownBy(() -> new DescriptorTemplate("x".repeat(101), "X", List.of(), "content"))
            .isInstanceOf(AgentValidationException.class);
    }

    @Test void content_rejects_bidi_override() {
        assertThatThrownBy(() -> new DescriptorTemplate("x", "X", List.of(), "text‪hidden"))
            .isInstanceOf(AgentValidationException.class);
    }
}

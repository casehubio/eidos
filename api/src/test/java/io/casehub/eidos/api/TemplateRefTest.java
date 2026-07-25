package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class TemplateRefTest {

    @Test void valid_ref_no_args() {
        var ref = new TemplateRef("style-guide", Map.of());
        assertThat(ref.templateId()).isEqualTo("style-guide");
        assertThat(ref.args()).isEmpty();
    }

    @Test void valid_ref_with_args() {
        var ref = new TemplateRef("villain", Map.of("catchphrase", "Nyah-ha-ha!"));
        assertThat(ref.args()).containsEntry("catchphrase", "Nyah-ha-ha!");
    }

    @Test void null_args_defaulted_to_empty() {
        var ref = new TemplateRef("x", null);
        assertThat(ref.args()).isEmpty();
    }

    @Test void args_are_immutable() {
        var args = new java.util.HashMap<>(Map.of("k", "v"));
        var ref = new TemplateRef("x", args);
        assertThatThrownBy(() -> ref.args().put("k2", "v2")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test void null_templateId_throws() {
        assertThatThrownBy(() -> new TemplateRef(null, Map.of()))
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName()).isEqualTo("templateRef.templateId"));
    }

    @Test void arg_value_rejects_over_1000_chars() {
        assertThatThrownBy(() -> new TemplateRef("x", Map.of("k", "v".repeat(1001))))
            .isInstanceOf(AgentValidationException.class);
    }
}

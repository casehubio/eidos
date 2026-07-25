package io.casehub.eidos.examples;

import io.casehub.eidos.api.AgentPromptContext;
import io.casehub.eidos.api.AgentRegistry;
import io.casehub.eidos.api.SystemPromptRenderer;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import io.casehub.eidos.api.TemplateRegistry;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class DescriptorTemplateScenarioTest {

    @Inject AgentRegistry registry;
    @Inject TemplateRegistry templateRegistry;
    @Inject SystemPromptRenderer renderer;

    @Test void templates_loaded_from_classpath() {
        assertThat(templateRegistry.resolve("document-review-conventions")).isPresent();
        assertThat(templateRegistry.resolve("communication-style")).isPresent();
    }

    @Test void communication_style_template_has_parameters() {
        var t = templateRegistry.resolve("communication-style").orElseThrow();
        assertThat(t.parameters()).containsExactly("formality", "feedback_approach");
    }

    @Test void descriptor_with_templates_registered() {
        var desc = registry.findById("drafthouse-structural-reviewer", "drafthouse");
        assertThat(desc).isPresent();
        assertThat(desc.get().templates()).isNotNull().hasSize(2);
        assertThat(desc.get().templates().get(0).templateId()).isEqualTo("document-review-conventions");
        assertThat(desc.get().templates().get(1).templateId()).isEqualTo("communication-style");
        assertThat(desc.get().templates().get(1).args()).containsEntry("formality", "professional");
    }

    @Test void rendered_markdown_includes_template_content() {
        var desc = registry.findById("drafthouse-structural-reviewer", "drafthouse").orElseThrow();
        var ctx = AgentPromptContext.forFormat(RenderFormat.MARKDOWN);
        var rendered = renderer.render(desc, ctx);
        assertThat(rendered.content()).contains("specific line references");
        assertThat(rendered.content()).contains("professional register");
        assertThat(rendered.content()).contains("collaborative approach");
    }

    @Test void rendered_prose_includes_template_content() {
        var desc = registry.findById("drafthouse-structural-reviewer", "drafthouse").orElseThrow();
        var ctx = AgentPromptContext.forFormat(RenderFormat.PROSE);
        var rendered = renderer.render(desc, ctx);
        assertThat(rendered.content()).contains("specific line references");
        assertThat(rendered.content()).contains("professional register");
    }

    @Test void parameterized_template_substituted() {
        var desc = registry.findById("drafthouse-structural-reviewer", "drafthouse").orElseThrow();
        var ctx = AgentPromptContext.forFormat(RenderFormat.MARKDOWN);
        var rendered = renderer.render(desc, ctx);
        assertThat(rendered.content()).doesNotContain("${formality}");
        assertThat(rendered.content()).doesNotContain("${feedback_approach}");
    }

    @Test void a2a_card_does_not_include_template_content() {
        var desc = registry.findById("drafthouse-structural-reviewer", "drafthouse").orElseThrow();
        var ctx = AgentPromptContext.forFormat(RenderFormat.A2A_CARD);
        var rendered = renderer.render(desc, ctx);
        assertThat(rendered.content()).doesNotContain("specific line references");
        assertThat(rendered.content()).doesNotContain("professional register");
    }
}

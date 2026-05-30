package io.casehub.eidos.eval;

import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EvalDatasetTest {

    @Test
    void all_returns_non_empty_list() {
        assertThat(EvalDataset.all()).isNotEmpty();
    }

    @Test
    void all_cases_have_valid_descriptors() {
        EvalDataset.all().forEach(c -> {
            assertThat(c.descriptor().agentId()).isNotBlank();
            assertThat(c.descriptor().name()).isNotBlank();
            assertThat(c.descriptor().slot()).isNotBlank();
            assertThat(c.descriptor().tenancyId()).isNotBlank();
        });
    }

    @Test
    void all_cases_use_claude_md_format() {
        EvalDataset.all().forEach(c ->
            assertThat(c.context().format()).isEqualTo(RenderFormat.CLAUDE_MD));
    }

    @Test
    void includes_minimal_and_maximal_cases() {
        final var names = EvalDataset.all().stream().map(EvalCase::name).toList();
        assertThat(names).contains("minimal", "maximal");
    }
}

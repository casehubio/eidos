package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AssembledPromptTest {

    @Test
    void recordFields_accessible() {
        var prompt = new AssembledPrompt("system content", "user content");
        assertThat(prompt.systemPrompt()).isEqualTo("system content");
        assertThat(prompt.userMessage()).isEqualTo("user content");
    }

    @Test
    void nullSystemPrompt_allowed() {
        var prompt = new AssembledPrompt(null, "user content");
        assertThat(prompt.systemPrompt()).isNull();
    }

    @Test
    void nullUserMessage_allowed() {
        var prompt = new AssembledPrompt("system content", null);
        assertThat(prompt.userMessage()).isNull();
    }

    @Test
    void threeArgConstructor_setsModel() {
        var prompt = new AssembledPrompt("system", "user", "claude-sonnet-4-20250514");
        assertThat(prompt.model()).isEqualTo("claude-sonnet-4-20250514");
        assertThat(prompt.systemPrompt()).isEqualTo("system");
        assertThat(prompt.userMessage()).isEqualTo("user");
    }

    @Test
    void twoArgConstructor_modelIsNull() {
        var prompt = new AssembledPrompt("system", "user");
        assertThat(prompt.model()).isNull();
    }

    @Test
    void nullModel_allowed() {
        var prompt = new AssembledPrompt("system", "user", null);
        assertThat(prompt.model()).isNull();
    }

    @Test
    void withModel_returnsNewInstanceWithModel() {
        var original  = new AssembledPrompt("system", "user");
        var withModel = original.withModel("claude-sonnet-4-20250514");
        assertThat(withModel.model()).isEqualTo("claude-sonnet-4-20250514");
        assertThat(withModel.systemPrompt()).isEqualTo("system");
        assertThat(withModel.userMessage()).isEqualTo("user");
        assertThat(original.model()).isNull();
    }

    @Test
    void withModel_null_clearsModel() {
        var prompt  = new AssembledPrompt("system", "user", "claude-sonnet-4-20250514");
        var cleared = prompt.withModel(null);
        assertThat(cleared.model()).isNull();
    }
}

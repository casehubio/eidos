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
}

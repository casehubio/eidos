package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PromptBlockTest {

    @Test
    void identityFactory_createsTierIdentity_nullTag() {
        var block = PromptBlock.identity("Agent identity content");
        assertThat(block.tier()).isEqualTo(PromptTier.IDENTITY);
        assertThat(block.tag()).isNull();
        assertThat(block.salience()).isEqualTo(0f);
        assertThat(block.content()).isEqualTo("Agent identity content");
    }

    @Test
    void cognitiveFactory_createsTierCognitive_withTag() {
        var block = PromptBlock.cognitive("MOOD", "warmth: 0.7");
        assertThat(block.tier()).isEqualTo(PromptTier.COGNITIVE);
        assertThat(block.tag()).isEqualTo("MOOD");
        assertThat(block.salience()).isEqualTo(0f);
        assertThat(block.content()).isEqualTo("warmth: 0.7");
    }

    @Test
    void commandFactory_createsTierCommand_doTag() {
        var block = PromptBlock.command("Call MCP gardenSearch");
        assertThat(block.tier()).isEqualTo(PromptTier.COMMAND);
        assertThat(block.tag()).isEqualTo("DO");
        assertThat(block.content()).isEqualTo("Call MCP gardenSearch");
    }

    @Test
    void conversationFactory_createsTierConversation_nullTag() {
        var block = PromptBlock.conversation("Hey, how are you?");
        assertThat(block.tier()).isEqualTo(PromptTier.CONVERSATION);
        assertThat(block.tag()).isNull();
        assertThat(block.content()).isEqualTo("Hey, how are you?");
    }

    @Test
    void nullTier_throws() {
        assertThatThrownBy(() -> new PromptBlock(null, null, 0f, "content"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void nullContent_throws() {
        assertThatThrownBy(() -> new PromptBlock(PromptTier.IDENTITY, null, 0f, null))
                .isInstanceOf(AgentValidationException.class)
                .hasMessageContaining("content");
    }

    @Test
    void blankContent_throws() {
        assertThatThrownBy(() -> new PromptBlock(PromptTier.IDENTITY, null, 0f, "  "))
                .isInstanceOf(AgentValidationException.class)
                .hasMessageContaining("content");
    }

    @Test
    void customSalience_preserved() {
        var block = new PromptBlock(PromptTier.COGNITIVE, "MOOD", 0.8f, "high salience");
        assertThat(block.salience()).isEqualTo(0.8f);
    }

    @Test
    void tierIsSystemPrompt_identityOnly() {
        assertThat(PromptTier.IDENTITY.isSystemPrompt()).isTrue();
        assertThat(PromptTier.COGNITIVE.isSystemPrompt()).isFalse();
        assertThat(PromptTier.COMMAND.isSystemPrompt()).isFalse();
        assertThat(PromptTier.CONVERSATION.isSystemPrompt()).isFalse();
    }
}

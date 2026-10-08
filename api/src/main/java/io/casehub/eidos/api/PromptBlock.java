package io.casehub.eidos.api;

import java.util.Objects;

public record PromptBlock(
        PromptTier tier,
        String tag,
        float salience,
        String content
) {
    public PromptBlock {
        Objects.requireNonNull(tier);
        if (content == null || content.isBlank()) {
            throw new AgentValidationException("promptBlock.content", "must not be null or blank");
        }
    }

    public static PromptBlock identity(String content) {
        return new PromptBlock(PromptTier.IDENTITY, null, 0f, content);
    }

    public static PromptBlock cognitive(String tag, String content) {
        return new PromptBlock(PromptTier.COGNITIVE, tag, 0f, content);
    }

    public static PromptBlock command(String content) {
        return new PromptBlock(PromptTier.COMMAND, "DO", 0f, content);
    }

    public static PromptBlock conversation(String content) {
        return new PromptBlock(PromptTier.CONVERSATION, null, 0f, content);
    }
}

package io.casehub.eidos.api;

public enum PromptTier {
    IDENTITY,
    COGNITIVE,
    COMMAND,
    CONVERSATION;

    public boolean isSystemPrompt() {
        return this == IDENTITY;
    }
}

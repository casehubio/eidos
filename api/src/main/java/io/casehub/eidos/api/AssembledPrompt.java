package io.casehub.eidos.api;

public record AssembledPrompt(
        String systemPrompt,
        String userMessage,
        String model
) {
    public AssembledPrompt(String systemPrompt, String userMessage) {
        this(systemPrompt, userMessage, null);
    }

    public AssembledPrompt withModel(String model) {
        return new AssembledPrompt(systemPrompt, userMessage, model);
    }
}

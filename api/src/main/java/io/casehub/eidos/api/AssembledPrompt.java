package io.casehub.eidos.api;

public record AssembledPrompt(
        String systemPrompt,
        String userMessage
) {}

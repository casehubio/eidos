package io.casehub.eidos.api;

public interface SystemPromptRenderer {
    RenderedPrompt render(AgentDescriptor descriptor, AgentPromptContext context);

    enum RenderFormat {
        CLAUDE_MD, OPENAI_SYSTEM, A2A_CARD, GEMINI
    }

    record RenderedPrompt(String content, RenderFormat format, String descriptorHash, String contextHash) {}
}

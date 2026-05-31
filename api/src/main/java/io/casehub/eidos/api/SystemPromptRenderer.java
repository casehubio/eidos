package io.casehub.eidos.api;

public interface SystemPromptRenderer {
    RenderedPrompt render(AgentDescriptor descriptor, AgentPromptContext context);

    enum RenderFormat {
        MARKDOWN,   // rich markdown — Claude and other markdown-capable models
        PROSE,      // flowing paragraphs — OpenAI, Gemini, Grok, Qwen, Mistral, Llama, ...
        A2A_CARD    // JSON — machine-readable agent identity card
    }

    record RenderedPrompt(String content, RenderFormat format, String descriptorHash, String contextHash) {}
}

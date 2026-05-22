package io.casehub.eidos.api;

public interface SystemPromptRenderer {
    RenderedPrompt render(AgentDescriptor descriptor, String goal, RenderContext context);

    record RenderContext(String situationalContext, RenderFormat format) {
        public static RenderContext claudeMd(String situationalContext) {
            return new RenderContext(situationalContext, RenderFormat.CLAUDE_MD);
        }
    }

    enum RenderFormat {
        CLAUDE_MD, OPENAI_SYSTEM, A2A_CARD, GEMINI
    }

    record RenderedPrompt(String content, RenderFormat format, String descriptorHash, String contextHash) {}
}

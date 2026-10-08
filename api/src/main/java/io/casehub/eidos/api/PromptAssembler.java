package io.casehub.eidos.api;

public interface PromptAssembler {
    AssembledPrompt assemble(AgentDescriptor descriptor, AgentPromptContext context);
}

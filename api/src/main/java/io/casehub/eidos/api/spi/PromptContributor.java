package io.casehub.eidos.api.spi;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentPromptContext;
import io.casehub.eidos.api.PromptBlock;

import java.util.List;

@FunctionalInterface
public interface PromptContributor {
    List<PromptBlock> contribute(AgentDescriptor descriptor, AgentPromptContext context);
}

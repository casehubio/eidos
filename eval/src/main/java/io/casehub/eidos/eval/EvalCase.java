package io.casehub.eidos.eval;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentPromptContext;

public record EvalCase(String name, AgentDescriptor descriptor, AgentPromptContext context) {}

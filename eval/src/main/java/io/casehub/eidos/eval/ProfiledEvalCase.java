package io.casehub.eidos.eval;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentPromptContext;

public record ProfiledEvalCase(
    String name,
    AgentDescriptor descriptor,
    AgentPromptContext context,
    AgentProfile profile
) implements EvalCase {}

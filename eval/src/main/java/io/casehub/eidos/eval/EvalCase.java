package io.casehub.eidos.eval;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentPromptContext;

public sealed interface EvalCase permits SyntheticEvalCase, ProfiledEvalCase {
    String name();
    AgentDescriptor descriptor();
    AgentPromptContext context();
}

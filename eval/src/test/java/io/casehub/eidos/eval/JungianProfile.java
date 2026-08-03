package io.casehub.eidos.eval;

import io.casehub.eidos.api.AgentDescriptor;

record JungianProfile(
    String name,
    String role,
    String domain,
    String sourceType,
    String mbtiType,
    String dominantFunction,
    String auxiliaryFunction,
    AgentDescriptor descriptor
) {}

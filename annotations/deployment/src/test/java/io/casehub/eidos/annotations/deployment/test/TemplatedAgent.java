package io.casehub.eidos.annotations.deployment.test;

import io.casehub.eidos.annotations.*;

@Identity(slot = "templated-agent", briefing = "Agent with templates")
@AgentTemplateRef(id = "safety-primer",
    args = {@TemplateArg(key = "domain", value = "legal")})
@AgentTemplateRef(id = "jurisdiction-notice",
    args = {@TemplateArg(key = "region", value = "EU")})
public interface TemplatedAgent {}

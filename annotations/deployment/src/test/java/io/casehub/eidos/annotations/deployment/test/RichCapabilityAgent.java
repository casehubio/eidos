package io.casehub.eidos.annotations.deployment.test;

import io.casehub.eidos.annotations.*;

@Identity(slot = "rich-cap-agent", briefing = "Agent with rich capabilities")
@AgentCapabilityDef(name = "analysis",
    description = "Deep analysis capability",
    qualityHint = 0.95,
    latencyHintP50Ms = 3000,
    costHint = "medium",
    inputTypes = {"application/pdf", "text/plain"},
    outputTypes = {"application/json"},
    tags = {"nlp", "extraction"},
    epistemicDomains = {
        @EpistemicDomain(value = "legal", score = 0.95),
        @EpistemicDomain(value = "financial", score = 0.6)
    },
    excludedDomains = {"criminal-law"})
@AgentCapabilityDef(name = "summarization",
    description = "Summarizes documents",
    qualityHint = 0.8)
public interface RichCapabilityAgent {}

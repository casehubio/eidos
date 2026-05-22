package io.casehub.eidos.api;

import java.util.List;

/**
 * Structured description of an individual LLM agent across four layers:
 * identity, slot, capabilities, and disposition.
 *
 * Self-declared at registration; validated over time by peer attestations and trust scores.
 * The descriptor is a prior — evidence updates it.
 */
public record AgentDescriptor(
        String agentId,
        String name,
        String version,
        String provider,
        String modelFamily,
        String modelVersion,
        String weightsFingerprint,
        String domainVocabulary,
        String slotVocabulary,
        String dispositionVocabulary,
        String slot,
        List<AgentCapability> capabilities,
        AgentDisposition disposition,
        String jurisdiction,
        String dataHandlingPolicy
) {}

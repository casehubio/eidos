package io.casehub.eidos.api;

import java.time.Instant;

public record AgentTask(
    String taskId,
    String agentId,
    String tenancyId,
    String capabilityTag,
    String taskDomain,
    String externalRef,   // opaque; nullable
    Instant startedAt,
    Instant endedAt       // null if in progress
) {}

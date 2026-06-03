package io.casehub.eidos.api;

import java.time.Instant;

public record AttestationRef(
    String taskId,           // null for backfilled refs without a matched task
    String agentId,
    String tenancyId,
    String ledgerEntryHash,
    String entryType,
    Instant attestedAt
) {}

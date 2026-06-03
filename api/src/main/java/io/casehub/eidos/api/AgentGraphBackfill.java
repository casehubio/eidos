package io.casehub.eidos.api;

import java.time.Instant;

public interface AgentGraphBackfill {
    BackfillResult backfillAgent(String agentId, String tenancyId);
    BackfillResult backfillAll(String tenancyId);
    BackfillResult backfillDelta(String tenancyId, Instant since);
}

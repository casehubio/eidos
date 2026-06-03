package io.casehub.eidos.runtime.graph;

import io.casehub.eidos.api.*;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;

@DefaultBean
@ApplicationScoped
public class NoOpAgentGraphBackfill implements AgentGraphBackfill {

    @Override
    public BackfillResult backfillAgent(final String agentId, final String tenancyId) {
        return new BackfillResult(0, 0, null, null);
    }

    @Override
    public BackfillResult backfillAll(final String tenancyId) {
        return new BackfillResult(0, 0, null, null);
    }

    @Override
    public BackfillResult backfillDelta(final String tenancyId, final Instant since) {
        return new BackfillResult(0, 0, null, null);
    }
}

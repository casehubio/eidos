package io.casehub.eidos.graph;

import io.casehub.eidos.api.*;
import jakarta.enterprise.context.ApplicationScoped;
// @Transactional omitted — stub methods do no DB work; add when real backfill is wired
import java.time.Instant;

@ApplicationScoped
public class JpaAgentGraphBackfill implements AgentGraphBackfill {

    @Override
    public BackfillResult backfillAgent(final String agentId, final String tenancyId) {
        // Production: query TrustExportService from casehub-ledger and import attestations.
        // That wiring requires casehub-ledger as a runtime dep — deferred to engine integration issue.
        return new BackfillResult(0, 0, null, null);
    }

    @Override
    public BackfillResult backfillAll(final String tenancyId) {
        return new BackfillResult(0, 0, null, null);
    }

    @Override
    public BackfillResult backfillDelta(final String tenancyId, final Instant since) {
        return new BackfillResult(0, 0, since, Instant.now());
    }
}

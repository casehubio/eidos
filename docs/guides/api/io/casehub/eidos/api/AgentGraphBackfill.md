# io.casehub.eidos.api.AgentGraphBackfill

**Package:** `io.casehub.eidos.api`

**Kind:** `interface`

## Methods

### `public abstract io.casehub.eidos.api.BackfillResult backfillAgent(java.lang.String agentId, java.lang.String tenancyId)`

#### Parameters

- `agentId` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)

### `public abstract io.casehub.eidos.api.BackfillResult backfillAll(java.lang.String tenancyId)`

#### Parameters

- `tenancyId` (`java.lang.String`)

### `public abstract io.casehub.eidos.api.BackfillResult backfillDelta(java.lang.String tenancyId, java.time.Instant since)`

#### Parameters

- `tenancyId` (`java.lang.String`)
- `since` (`java.time.Instant`)

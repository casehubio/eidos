# io.casehub.eidos.api.AgentStateStore

**Package:** `io.casehub.eidos.api`

**Kind:** `interface`

## Methods

### `public abstract void clear(java.lang.String agentId, java.lang.String tenancyId)`

#### Parameters

- `agentId` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)

### `public abstract java.util.Optional<io.casehub.eidos.api.DegradationReason> query(java.lang.String agentId, java.lang.String tenancyId)`

#### Parameters

- `agentId` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)

### `public abstract void record(java.lang.String agentId, java.lang.String tenancyId, io.casehub.eidos.api.DegradationReason reason, java.time.Instant expiresAt)`

#### Parameters

- `agentId` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)
- `reason` (`io.casehub.eidos.api.DegradationReason`)
- `expiresAt` (`java.time.Instant`)

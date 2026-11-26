# io.casehub.eidos.api.DetectedRoleSignalStore

**Package:** `io.casehub.eidos.api`

**Kind:** `interface`

## Methods

### `public abstract void clear(java.lang.String agentId, java.lang.String tenancyId)`

#### Parameters

- `agentId` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)

### `public abstract int purge(java.lang.String agentId, java.lang.String tenancyId, java.time.Instant olderThan)`

#### Parameters

- `agentId` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)
- `olderThan` (`java.time.Instant`)

### `public abstract void record(io.casehub.eidos.api.DetectedRoleSignal signal)`

#### Parameters

- `signal` (`io.casehub.eidos.api.DetectedRoleSignal`)

### `public abstract java.util.List<io.casehub.eidos.api.DetectedRoleSignal> signals(java.lang.String agentId, java.lang.String roleName, java.lang.String tenancyId, java.time.Instant since)`

#### Parameters

- `agentId` (`java.lang.String`)
- `roleName` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)
- `since` (`java.time.Instant`)

### `public abstract java.util.List<io.casehub.eidos.api.DetectedRoleSignal> signalsByAgent(java.lang.String agentId, java.lang.String tenancyId, java.time.Instant since)`

#### Parameters

- `agentId` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)
- `since` (`java.time.Instant`)

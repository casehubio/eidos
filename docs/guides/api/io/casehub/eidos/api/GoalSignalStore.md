# io.casehub.eidos.api.GoalSignalStore

**Package:** `io.casehub.eidos.api`

**Kind:** `interface`

## Methods

### `public abstract void clear(java.lang.String agentId, java.lang.String tenancyId)`

#### Parameters

- `agentId` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)

### `public abstract void decay(java.lang.String agentId, java.lang.String tenancyId, double decayFactor)`

#### Parameters

- `agentId` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)
- `decayFactor` (`double`)

### `public abstract java.util.Map<java.lang.String,io.casehub.eidos.api.GoalOutcomeCounts> outcomeCounts(java.lang.String agentId, java.lang.String tenancyId)`

#### Parameters

- `agentId` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)

### `public abstract void recordOutcome(java.lang.String agentId, java.lang.String tenancyId, java.lang.String goalName, io.casehub.eidos.api.GoalOutcome outcome)`

#### Parameters

- `agentId` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)
- `goalName` (`java.lang.String`)
- `outcome` (`io.casehub.eidos.api.GoalOutcome`)

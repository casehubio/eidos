# io.casehub.eidos.api.InMemoryGoalSignalStore

**Package:** `io.casehub.eidos.api`

**Kind:** `class`

## Fields

### `store` (`java.util.concurrent.ConcurrentHashMap<java.lang.String,java.util.concurrent.ConcurrentHashMap<java.lang.String,int[]>>`)

## Constructors

### `public InMemoryGoalSignalStore()`

## Methods

### `public void clear(java.lang.String agentId, java.lang.String tenancyId)`

#### Parameters

- `agentId` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)

### `public void decay(java.lang.String agentId, java.lang.String tenancyId, double decayFactor)`

#### Parameters

- `agentId` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)
- `decayFactor` (`double`)

### `public java.util.Map<java.lang.String,io.casehub.eidos.api.GoalOutcomeCounts> outcomeCounts(java.lang.String agentId, java.lang.String tenancyId)`

#### Parameters

- `agentId` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)

### `public void recordOutcome(java.lang.String agentId, java.lang.String tenancyId, java.lang.String goalName, io.casehub.eidos.api.GoalOutcome outcome)`

#### Parameters

- `agentId` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)
- `goalName` (`java.lang.String`)
- `outcome` (`io.casehub.eidos.api.GoalOutcome`)

# io.casehub.eidos.api.AgentRegistry

**Package:** `io.casehub.eidos.api`

**Kind:** `interface`

## Methods

### `public default void deregister(java.lang.String agentId, java.lang.String tenancyId)`

#### Parameters

- `agentId` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)

### `public abstract java.util.List<io.casehub.eidos.api.AgentMatch> find(io.casehub.eidos.api.AgentQuery query)`

Finds agents matching the query criteria.

<p>When `AgentQuery.capabilityName()` is non-null, results carry
`AgentMatch.resolvedCapability()` and are ordered by match quality
(best first per OWLS-MX ordering). When no capability is queried,
`resolvedCapability` is null and ordering is unspecified.

#### Parameters

- `query` (`io.casehub.eidos.api.AgentQuery`)

### `public abstract java.util.Optional<io.casehub.eidos.api.AgentDescriptor> findById(java.lang.String agentId, java.lang.String tenancyId)`

#### Parameters

- `agentId` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)

#### Throws

- `NullPointerException` — if agentId or tenancyId is null

### `public default java.util.Optional<io.casehub.eidos.api.AgentDescriptor> findEffective(java.lang.String agentId, java.lang.String tenancyId)`

Returns the descriptor with any evolved disposition composed on top of
the authored baseline. The default returns the authored descriptor as-is;
Runtime implementations compose via `LivedDispositionSource`.

#### Parameters

- `agentId` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)

### `public abstract void register(io.casehub.eidos.api.AgentDescriptor descriptor)`

#### Parameters

- `descriptor` (`io.casehub.eidos.api.AgentDescriptor`)

### `public default void updateGoalLifecycleState(java.lang.String agentId, java.lang.String tenancyId, java.lang.String goalName, io.casehub.eidos.api.GoalLifecycleState newState)`

#### Parameters

- `agentId` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)
- `goalName` (`java.lang.String`)
- `newState` (`io.casehub.eidos.api.GoalLifecycleState`)

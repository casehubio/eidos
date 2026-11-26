# io.casehub.eidos.api.AgentGraphQuery

**Package:** `io.casehub.eidos.api`

**Kind:** `interface`

## Methods

### `public abstract io.casehub.eidos.api.AgentTaskHistory agentHistory(java.lang.String agentId, java.lang.String tenancyId)`

Returns all tasks including in-progress (endedAt=null).

#### Parameters

- `agentId` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)

### `public default io.casehub.eidos.api.AgentTaskHistory agentHistory(java.lang.String agentId, java.lang.String tenancyId, java.time.Instant since)`

#### Parameters

- `agentId` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)
- `since` (`java.time.Instant`)

### `public abstract java.util.List<io.casehub.eidos.api.AttestationRef> attestationsFor(java.lang.String agentId, java.lang.String tenancyId)`

#### Parameters

- `agentId` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)

### `public abstract java.util.List<java.lang.String> coActiveAgents(java.lang.String externalRef, java.lang.String tenancyId)`

#### Parameters

- `externalRef` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)

### `public abstract io.casehub.eidos.api.AgentTaskHistory historyByCapability(java.lang.String agentId, java.lang.String capabilityTag, java.lang.String tenancyId)`

#### Parameters

- `agentId` (`java.lang.String`)
- `capabilityTag` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)

### `public default io.casehub.eidos.api.AgentTaskHistory historyByCapability(java.lang.String agentId, java.lang.String capabilityTag, java.lang.String tenancyId, java.time.Instant since)`

#### Parameters

- `agentId` (`java.lang.String`)
- `capabilityTag` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)
- `since` (`java.time.Instant`)

### `public abstract io.casehub.eidos.api.AgentTaskHistory historyByRole(java.lang.String agentId, java.lang.String roleName, java.lang.String tenancyId, java.time.Instant since)`

#### Parameters

- `agentId` (`java.lang.String`)
- `roleName` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)
- `since` (`java.time.Instant`)

### `public default io.casehub.eidos.api.AgentTaskHistory historyByRole(java.lang.String agentId, java.lang.String roleName, java.lang.String tenancyId, java.time.Instant since, java.lang.String taskDomain)`

#### Parameters

- `agentId` (`java.lang.String`)
- `roleName` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)
- `since` (`java.time.Instant`)
- `taskDomain` (`java.lang.String`)

### `public abstract java.util.List<java.lang.String> topAgentsByOutcome(java.lang.String capabilityTag, java.lang.String taskDomain, java.lang.String tenancyId, int limit)`

Returns agentIds ranked by Wilson lower bound score.
quality = confidence × multiplier (SUCCEEDED=1.0, PARTIALLY=0.5, FAILED=0.0)
Wilson z=1.645. Score=0 when no observations. Agents with score=0 appear last.

#### Parameters

- `capabilityTag` (`java.lang.String`)
- `taskDomain` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)
- `limit` (`int`)

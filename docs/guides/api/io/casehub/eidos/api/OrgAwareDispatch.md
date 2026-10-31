# io.casehub.eidos.api.OrgAwareDispatch

**Package:** `io.casehub.eidos.api`

**Kind:** `interface`

## Methods

### `public abstract boolean canDelegateTo(java.lang.String fromAgentId, java.lang.String toAgentId, java.lang.String tenancyId)`

#### Parameters

- `fromAgentId` (`java.lang.String`)
- `toAgentId` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)

### `public abstract java.util.Optional<io.casehub.eidos.api.AgentMatch> escalate(java.lang.String agentId, java.lang.String capabilityName, java.lang.String tenancyId)`

#### Parameters

- `agentId` (`java.lang.String`)
- `capabilityName` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)

### `public abstract int orgDistance(java.lang.String agentIdA, java.lang.String agentIdB, java.lang.String tenancyId)`

#### Parameters

- `agentIdA` (`java.lang.String`)
- `agentIdB` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)

### `public abstract java.util.List<java.lang.String> teamMembers(java.lang.String agentId, java.lang.String tenancyId)`

#### Parameters

- `agentId` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)

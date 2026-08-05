# io.casehub.eidos.api.AgentRegistry

**Package:** `io.casehub.eidos.api`

**Kind:** `interface`

## Methods

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

### `public abstract void register(io.casehub.eidos.api.AgentDescriptor descriptor)`

#### Parameters

- `descriptor` (`io.casehub.eidos.api.AgentDescriptor`)

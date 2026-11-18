# io.casehub.eidos.api.LivedDispositionSource

**Package:** `io.casehub.eidos.api`

**Kind:** `interface`

## Methods

### `public abstract io.casehub.eidos.api.AgentDisposition resolveEffective(java.lang.String agentId, java.lang.String tenancyId, io.casehub.eidos.api.AgentDisposition authored)`

Returns the effective disposition for the given agent, composing any
evolved personality state on top of the authored baseline.

<p>When no evolution exists, returns `authored` as-is.
Implementations receive the authored disposition so they can perform
fingerprint checks against the authored baseline without calling back
to the registry.

#### Parameters

- `agentId` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)
- `authored` (`io.casehub.eidos.api.AgentDisposition`)

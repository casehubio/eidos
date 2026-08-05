# io.casehub.eidos.api.AgentTaskHistory

**Package:** `io.casehub.eidos.api`

**Kind:** `record`

## Fields

### `agentId` (`java.lang.String`)

### `attestationRefs` (`java.util.List<io.casehub.eidos.api.AttestationRef>`)

### `outcomes` (`java.util.List<io.casehub.eidos.api.AgentOutcome>`)

### `sufficiency` (`io.casehub.eidos.api.GraphDataSufficiency`)

### `tasks` (`java.util.List<io.casehub.eidos.api.AgentTask>`)

### `tenancyId` (`java.lang.String`)

## Record Components

### `agentId` (`java.lang.String`)

### `attestationRefs` (`java.util.List<io.casehub.eidos.api.AttestationRef>`)

### `outcomes` (`java.util.List<io.casehub.eidos.api.AgentOutcome>`)

### `sufficiency` (`io.casehub.eidos.api.GraphDataSufficiency`)

### `tasks` (`java.util.List<io.casehub.eidos.api.AgentTask>`)

### `tenancyId` (`java.lang.String`)

## Constructors

### `public AgentTaskHistory(java.lang.String agentId, java.lang.String tenancyId, java.util.List<io.casehub.eidos.api.AgentTask> tasks, java.util.List<io.casehub.eidos.api.AgentOutcome> outcomes, java.util.List<io.casehub.eidos.api.AttestationRef> attestationRefs, io.casehub.eidos.api.GraphDataSufficiency sufficiency)`

#### Parameters

- `agentId` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)
- `tasks` (`java.util.List<io.casehub.eidos.api.AgentTask>`)
- `outcomes` (`java.util.List<io.casehub.eidos.api.AgentOutcome>`)
- `attestationRefs` (`java.util.List<io.casehub.eidos.api.AttestationRef>`)
- `sufficiency` (`io.casehub.eidos.api.GraphDataSufficiency`)

## Methods

### `public java.lang.String agentId()`

### `public java.util.List<io.casehub.eidos.api.AttestationRef> attestationRefs()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public java.util.List<io.casehub.eidos.api.AgentOutcome> outcomes()`

### `public io.casehub.eidos.api.GraphDataSufficiency sufficiency()`

### `public java.util.List<io.casehub.eidos.api.AgentTask> tasks()`

### `public java.lang.String tenancyId()`

### `public final java.lang.String toString()`

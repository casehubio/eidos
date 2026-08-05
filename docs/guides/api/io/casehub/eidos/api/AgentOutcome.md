# io.casehub.eidos.api.AgentOutcome

**Package:** `io.casehub.eidos.api`

**Kind:** `record`

## Fields

### `confidence` (`double`)

### `degradationReason` (`io.casehub.eidos.api.DegradationReason`)

### `observedAt` (`java.time.Instant`)

### `result` (`io.casehub.eidos.api.TaskResult`)

### `taskId` (`java.lang.String`)

## Record Components

### `confidence` (`double`)

### `degradationReason` (`io.casehub.eidos.api.DegradationReason`)

### `observedAt` (`java.time.Instant`)

### `result` (`io.casehub.eidos.api.TaskResult`)

### `taskId` (`java.lang.String`)

## Constructors

### `public AgentOutcome(java.lang.String taskId, io.casehub.eidos.api.TaskResult result, double confidence, java.time.Instant observedAt, io.casehub.eidos.api.DegradationReason degradationReason)`

#### Parameters

- `taskId` (`java.lang.String`)
- `result` (`io.casehub.eidos.api.TaskResult`)
- `confidence` (`double`)
- `observedAt` (`java.time.Instant`)
- `degradationReason` (`io.casehub.eidos.api.DegradationReason`)

## Methods

### `public double confidence()`

### `public io.casehub.eidos.api.DegradationReason degradationReason()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public java.time.Instant observedAt()`

### `public io.casehub.eidos.api.TaskResult result()`

### `public java.lang.String taskId()`

### `public final java.lang.String toString()`

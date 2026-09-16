# io.casehub.eidos.api.AgentSelection.Selected

**Package:** `io.casehub.eidos.api`

**Kind:** `record`

## Fields

### `agent` (`io.casehub.eidos.api.AgentDescriptor`)

### `reason` (`java.lang.String`)

### `resolvedCapability` (`io.casehub.eidos.api.ResolvedCapability`)

### `trustScore` (`double`)

## Record Components

### `agent` (`io.casehub.eidos.api.AgentDescriptor`)

### `reason` (`java.lang.String`)

### `resolvedCapability` (`io.casehub.eidos.api.ResolvedCapability`)

the capability resolution result, or null when no capability was queried

### `trustScore` (`double`)

## Constructors

### `public Selected(io.casehub.eidos.api.AgentDescriptor agent, io.casehub.eidos.api.ResolvedCapability resolvedCapability, double trustScore, java.lang.String reason)`

#### Parameters

- `agent` (`io.casehub.eidos.api.AgentDescriptor`)
- `resolvedCapability` (`io.casehub.eidos.api.ResolvedCapability`)
- `trustScore` (`double`)
- `reason` (`java.lang.String`)

## Methods

### `public io.casehub.eidos.api.AgentDescriptor agent()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public java.lang.String reason()`

### `public io.casehub.eidos.api.ResolvedCapability resolvedCapability()`

### `public final java.lang.String toString()`

### `public double trustScore()`

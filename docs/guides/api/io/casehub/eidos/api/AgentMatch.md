# io.casehub.eidos.api.AgentMatch

**Package:** `io.casehub.eidos.api`

**Kind:** `record`

Result of `AgentRegistry.find(AgentQuery)` — an agent that matched the query,
with optional capability resolution context.

<p>`resolvedCapability` is non-null when `AgentQuery.capabilityName()` was specified,
carrying the declared capability that matched and the OWLS-MX match degree.
Null for slot-only or `AgentQuery.all` queries where no capability matching occurred.

## Fields

### `descriptor` (`io.casehub.eidos.api.AgentDescriptor`)

### `resolvedCapability` (`io.casehub.eidos.api.ResolvedCapability`)

## Record Components

### `descriptor` (`io.casehub.eidos.api.AgentDescriptor`)

the matched agent descriptor

### `resolvedCapability` (`io.casehub.eidos.api.ResolvedCapability`)

the capability resolution result, or null when no capability was queried

## Constructors

### `public AgentMatch(io.casehub.eidos.api.AgentDescriptor descriptor, io.casehub.eidos.api.ResolvedCapability resolvedCapability)`

#### Parameters

- `descriptor` (`io.casehub.eidos.api.AgentDescriptor`)
- `resolvedCapability` (`io.casehub.eidos.api.ResolvedCapability`)

## Methods

### `public io.casehub.eidos.api.AgentDescriptor descriptor()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public io.casehub.eidos.api.ResolvedCapability resolvedCapability()`

### `public final java.lang.String toString()`

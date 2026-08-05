# io.casehub.eidos.api.ResolvedCapability

**Package:** `io.casehub.eidos.api`

**Kind:** `record`

Result of resolving a capability tag against declared capabilities via
String, VocabularyRegistry).

## Fields

### `capability` (`io.casehub.eidos.api.AgentCapability`)

### `degree` (`io.casehub.eidos.api.MatchDegree`)

## Record Components

### `capability` (`io.casehub.eidos.api.AgentCapability`)

the declared capability that matched

### `degree` (`io.casehub.eidos.api.MatchDegree`)

the OWLS-MX match degree

## Constructors

### `public ResolvedCapability(io.casehub.eidos.api.AgentCapability capability, io.casehub.eidos.api.MatchDegree degree)`

#### Parameters

- `capability` (`io.casehub.eidos.api.AgentCapability`)
- `degree` (`io.casehub.eidos.api.MatchDegree`)

## Methods

### `public io.casehub.eidos.api.AgentCapability capability()`

### `public io.casehub.eidos.api.MatchDegree degree()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public final java.lang.String toString()`

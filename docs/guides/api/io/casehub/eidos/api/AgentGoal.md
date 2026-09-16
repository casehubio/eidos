# io.casehub.eidos.api.AgentGoal

**Package:** `io.casehub.eidos.api`

**Kind:** `record`

## Fields

### `attributes` (`java.util.Map<java.lang.String,java.lang.String>`)

### `capabilities` (`java.util.List<java.lang.String>`)

### `description` (`java.lang.String`)

### `name` (`java.lang.String`)

### `priority` (`io.casehub.eidos.api.GoalPriority`)

### `visibility` (`io.casehub.eidos.api.Visibility`)

## Record Components

### `attributes` (`java.util.Map<java.lang.String,java.lang.String>`)

### `capabilities` (`java.util.List<java.lang.String>`)

### `description` (`java.lang.String`)

### `name` (`java.lang.String`)

### `priority` (`io.casehub.eidos.api.GoalPriority`)

### `visibility` (`io.casehub.eidos.api.Visibility`)

## Constructors

### `public AgentGoal(java.lang.String name, java.lang.String description, io.casehub.eidos.api.GoalPriority priority, io.casehub.eidos.api.Visibility visibility, java.util.List<java.lang.String> capabilities, java.util.Map<java.lang.String,java.lang.String> attributes)`

#### Parameters

- `name` (`java.lang.String`)
- `description` (`java.lang.String`)
- `priority` (`io.casehub.eidos.api.GoalPriority`)
- `visibility` (`io.casehub.eidos.api.Visibility`)
- `capabilities` (`java.util.List<java.lang.String>`)
- `attributes` (`java.util.Map<java.lang.String,java.lang.String>`)

## Methods

### `public java.util.Map<java.lang.String,java.lang.String> attributes()`

### `public java.util.List<java.lang.String> capabilities()`

### `public java.lang.String description()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public java.lang.String name()`

### `public io.casehub.eidos.api.GoalPriority priority()`

### `public io.casehub.eidos.api.AgentGoal.Builder toBuilder()`

### `public final java.lang.String toString()`

### `public io.casehub.eidos.api.Visibility visibility()`

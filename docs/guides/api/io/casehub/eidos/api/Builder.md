# io.casehub.eidos.api.AgentGoal.Builder

**Package:** `io.casehub.eidos.api`

**Kind:** `class`

## Fields

### `attributes` (`java.util.Map<java.lang.String,java.lang.String>`)

### `capabilities` (`java.util.List<java.lang.String>`)

### `description` (`java.lang.String`)

### `horizon` (`io.casehub.eidos.api.GoalHorizon`)

### `lifecycleState` (`io.casehub.eidos.api.GoalLifecycleState`)

### `name` (`java.lang.String`)

### `priority` (`io.casehub.eidos.api.GoalPriority`)

### `visibility` (`io.casehub.eidos.api.Visibility`)

## Constructors

### `Builder(java.lang.String name, java.lang.String description, io.casehub.eidos.api.GoalPriority priority, io.casehub.eidos.api.Visibility visibility, java.util.List<java.lang.String> capabilities, java.util.Map<java.lang.String,java.lang.String> attributes, io.casehub.eidos.api.GoalLifecycleState lifecycleState, io.casehub.eidos.api.GoalHorizon horizon)`

#### Parameters

- `name` (`java.lang.String`)
- `description` (`java.lang.String`)
- `priority` (`io.casehub.eidos.api.GoalPriority`)
- `visibility` (`io.casehub.eidos.api.Visibility`)
- `capabilities` (`java.util.List<java.lang.String>`)
- `attributes` (`java.util.Map<java.lang.String,java.lang.String>`)
- `lifecycleState` (`io.casehub.eidos.api.GoalLifecycleState`)
- `horizon` (`io.casehub.eidos.api.GoalHorizon`)

## Methods

### `public io.casehub.eidos.api.AgentGoal.Builder attributes(java.util.Map<java.lang.String,java.lang.String> v)`

#### Parameters

- `v` (`java.util.Map<java.lang.String,java.lang.String>`)

### `public io.casehub.eidos.api.AgentGoal build()`

### `public io.casehub.eidos.api.AgentGoal.Builder capabilities(java.util.List<java.lang.String> v)`

#### Parameters

- `v` (`java.util.List<java.lang.String>`)

### `public io.casehub.eidos.api.AgentGoal.Builder description(java.lang.String v)`

#### Parameters

- `v` (`java.lang.String`)

### `public io.casehub.eidos.api.AgentGoal.Builder horizon(io.casehub.eidos.api.GoalHorizon v)`

#### Parameters

- `v` (`io.casehub.eidos.api.GoalHorizon`)

### `public io.casehub.eidos.api.AgentGoal.Builder lifecycleState(io.casehub.eidos.api.GoalLifecycleState v)`

#### Parameters

- `v` (`io.casehub.eidos.api.GoalLifecycleState`)

### `public io.casehub.eidos.api.AgentGoal.Builder name(java.lang.String v)`

#### Parameters

- `v` (`java.lang.String`)

### `public io.casehub.eidos.api.AgentGoal.Builder priority(io.casehub.eidos.api.GoalPriority v)`

#### Parameters

- `v` (`io.casehub.eidos.api.GoalPriority`)

### `public io.casehub.eidos.api.AgentGoal.Builder visibility(io.casehub.eidos.api.Visibility v)`

#### Parameters

- `v` (`io.casehub.eidos.api.Visibility`)

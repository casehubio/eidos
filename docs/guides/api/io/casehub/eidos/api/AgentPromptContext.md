# io.casehub.eidos.api.AgentPromptContext

**Package:** `io.casehub.eidos.api`

**Kind:** `record`

## Fields

### `format` (`io.casehub.eidos.api.SystemPromptRenderer.RenderFormat`)

### `goal` (`java.util.Optional<io.casehub.eidos.api.GoalContext>`)

### `resources` (`java.util.List<io.casehub.eidos.api.Resource>`)

### `situationalContext` (`java.lang.String`)

## Record Components

### `format` (`io.casehub.eidos.api.SystemPromptRenderer.RenderFormat`)

### `goal` (`java.util.Optional<io.casehub.eidos.api.GoalContext>`)

### `resources` (`java.util.List<io.casehub.eidos.api.Resource>`)

### `situationalContext` (`java.lang.String`)

## Constructors

### `public AgentPromptContext(java.util.Optional<io.casehub.eidos.api.GoalContext> goal, java.util.List<io.casehub.eidos.api.Resource> resources, java.lang.String situationalContext, io.casehub.eidos.api.SystemPromptRenderer.RenderFormat format)`

#### Parameters

- `goal` (`java.util.Optional<io.casehub.eidos.api.GoalContext>`)
- `resources` (`java.util.List<io.casehub.eidos.api.Resource>`)
- `situationalContext` (`java.lang.String`)
- `format` (`io.casehub.eidos.api.SystemPromptRenderer.RenderFormat`)

## Methods

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public static io.casehub.eidos.api.AgentPromptContext forFormat(io.casehub.eidos.api.SystemPromptRenderer.RenderFormat format)`

#### Parameters

- `format` (`io.casehub.eidos.api.SystemPromptRenderer.RenderFormat`)

### `public io.casehub.eidos.api.SystemPromptRenderer.RenderFormat format()`

### `public java.util.Optional<io.casehub.eidos.api.GoalContext> goal()`

### `public final int hashCode()`

### `public java.util.List<io.casehub.eidos.api.Resource> resources()`

### `public java.lang.String situationalContext()`

### `public final java.lang.String toString()`

### `public io.casehub.eidos.api.AgentPromptContext withGoal(io.casehub.eidos.api.GoalContext goal)`

#### Parameters

- `goal` (`io.casehub.eidos.api.GoalContext`)

### `public io.casehub.eidos.api.AgentPromptContext withResources(java.util.List<io.casehub.eidos.api.Resource> resources)`

#### Parameters

- `resources` (`java.util.List<io.casehub.eidos.api.Resource>`)

### `public io.casehub.eidos.api.AgentPromptContext withSituationalContext(java.lang.String situationalContext)`

#### Parameters

- `situationalContext` (`java.lang.String`)

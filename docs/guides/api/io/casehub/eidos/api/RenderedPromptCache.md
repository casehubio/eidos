# io.casehub.eidos.api.RenderedPromptCache

**Package:** `io.casehub.eidos.api`

**Kind:** `interface`

## Methods

### `public abstract java.util.Optional<io.casehub.eidos.api.SystemPromptRenderer.RenderedPrompt> get(java.lang.String cacheKey)`

#### Parameters

- `cacheKey` (`java.lang.String`)

### `public abstract void put(java.lang.String cacheKey, io.casehub.eidos.api.SystemPromptRenderer.RenderedPrompt result)`

Stores a rendered prompt. Must not throw — implementations handle errors internally
so a cache failure never aborts a render.

#### Parameters

- `cacheKey` (`java.lang.String`)
- `result` (`io.casehub.eidos.api.SystemPromptRenderer.RenderedPrompt`)

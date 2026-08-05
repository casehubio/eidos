# io.casehub.eidos.api.SystemPromptRenderer.RenderedPrompt

**Package:** `io.casehub.eidos.api`

**Kind:** `record`

## Fields

### `coherenceReport` (`io.casehub.eidos.api.CoherenceReport`)

### `content` (`java.lang.String`)

### `contextHash` (`java.lang.String`)

### `descriptorHash` (`java.lang.String`)

### `enriched` (`boolean`)

### `format` (`io.casehub.eidos.api.SystemPromptRenderer.RenderFormat`)

## Record Components

### `coherenceReport` (`io.casehub.eidos.api.CoherenceReport`)

### `content` (`java.lang.String`)

### `contextHash` (`java.lang.String`)

### `descriptorHash` (`java.lang.String`)

### `enriched` (`boolean`)

### `format` (`io.casehub.eidos.api.SystemPromptRenderer.RenderFormat`)

## Constructors

### `public RenderedPrompt(java.lang.String content, io.casehub.eidos.api.SystemPromptRenderer.RenderFormat format, java.lang.String descriptorHash, java.lang.String contextHash, boolean enriched)`

#### Parameters

- `content` (`java.lang.String`)
- `format` (`io.casehub.eidos.api.SystemPromptRenderer.RenderFormat`)
- `descriptorHash` (`java.lang.String`)
- `contextHash` (`java.lang.String`)
- `enriched` (`boolean`)

### `public RenderedPrompt(java.lang.String content, io.casehub.eidos.api.SystemPromptRenderer.RenderFormat format, java.lang.String descriptorHash, java.lang.String contextHash, boolean enriched, io.casehub.eidos.api.CoherenceReport coherenceReport)`

#### Parameters

- `content` (`java.lang.String`)
- `format` (`io.casehub.eidos.api.SystemPromptRenderer.RenderFormat`)
- `descriptorHash` (`java.lang.String`)
- `contextHash` (`java.lang.String`)
- `enriched` (`boolean`)
- `coherenceReport` (`io.casehub.eidos.api.CoherenceReport`)

## Methods

### `public io.casehub.eidos.api.CoherenceReport coherenceReport()`

### `public java.lang.String content()`

### `public java.lang.String contextHash()`

### `public java.lang.String descriptorHash()`

### `public boolean enriched()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public io.casehub.eidos.api.SystemPromptRenderer.RenderFormat format()`

### `public final int hashCode()`

### `public final java.lang.String toString()`

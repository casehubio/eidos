# io.casehub.eidos.api.AgentVoiceProfile

**Package:** `io.casehub.eidos.api`

**Kind:** `record`

## Fields

### `accent` (`java.lang.String`)

### `catchphrases` (`java.util.List<java.lang.String>`)

### `description` (`java.lang.String`)

### `personas` (`java.util.Map<java.lang.String,io.casehub.eidos.api.AgentVoiceProfile>`)

### `quirks` (`java.util.List<java.lang.String>`)

### `register` (`java.lang.String`)

### `speechPatterns` (`java.util.List<java.lang.String>`)

### `vocabularyAvoids` (`java.util.List<java.lang.String>`)

### `vocabularyUses` (`java.util.List<java.lang.String>`)

## Record Components

### `accent` (`java.lang.String`)

### `catchphrases` (`java.util.List<java.lang.String>`)

### `description` (`java.lang.String`)

### `personas` (`java.util.Map<java.lang.String,io.casehub.eidos.api.AgentVoiceProfile>`)

### `quirks` (`java.util.List<java.lang.String>`)

### `register` (`java.lang.String`)

### `speechPatterns` (`java.util.List<java.lang.String>`)

### `vocabularyAvoids` (`java.util.List<java.lang.String>`)

### `vocabularyUses` (`java.util.List<java.lang.String>`)

## Constructors

### `public AgentVoiceProfile(java.lang.String description, java.lang.String register, java.lang.String accent, java.util.List<java.lang.String> catchphrases, java.util.List<java.lang.String> speechPatterns, java.util.List<java.lang.String> vocabularyUses, java.util.List<java.lang.String> vocabularyAvoids, java.util.List<java.lang.String> quirks, java.util.Map<java.lang.String,io.casehub.eidos.api.AgentVoiceProfile> personas)`

#### Parameters

- `description` (`java.lang.String`)
- `register` (`java.lang.String`)
- `accent` (`java.lang.String`)
- `catchphrases` (`java.util.List<java.lang.String>`)
- `speechPatterns` (`java.util.List<java.lang.String>`)
- `vocabularyUses` (`java.util.List<java.lang.String>`)
- `vocabularyAvoids` (`java.util.List<java.lang.String>`)
- `quirks` (`java.util.List<java.lang.String>`)
- `personas` (`java.util.Map<java.lang.String,io.casehub.eidos.api.AgentVoiceProfile>`)

## Methods

### `public java.lang.String accent()`

### `public java.util.List<java.lang.String> catchphrases()`

### `public java.lang.String description()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public java.util.Map<java.lang.String,io.casehub.eidos.api.AgentVoiceProfile> personas()`

### `public java.util.List<java.lang.String> quirks()`

### `public java.lang.String register()`

### `public io.casehub.eidos.api.AgentVoiceProfile resolvePersona(java.lang.String personaName)`

#### Parameters

- `personaName` (`java.lang.String`)

### `public java.util.List<java.lang.String> speechPatterns()`

### `public final java.lang.String toString()`

### `public java.util.List<java.lang.String> vocabularyAvoids()`

### `public java.util.List<java.lang.String> vocabularyUses()`

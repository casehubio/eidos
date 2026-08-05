# io.casehub.eidos.api.BehavioralExpectations

**Package:** `io.casehub.eidos.api`

**Kind:** `class`

## Constructors

### `private BehavioralExpectations()`

## Methods

### `public static boolean delegationExpected(io.casehub.eidos.api.AgentDisposition disposition)`

#### Parameters

- `disposition` (`io.casehub.eidos.api.AgentDisposition`)

### `public static boolean escalationExpected(io.casehub.eidos.api.AgentDescriptor descriptor, io.casehub.eidos.api.VocabularyRegistry registry)`

#### Parameters

- `descriptor` (`io.casehub.eidos.api.AgentDescriptor`)
- `registry` (`io.casehub.eidos.api.VocabularyRegistry`)

### `public static boolean escalationExpected(io.casehub.eidos.api.AgentDisposition disposition, java.lang.String autonomyVocabUri, io.casehub.eidos.api.VocabularyRegistry registry)`

#### Parameters

- `disposition` (`io.casehub.eidos.api.AgentDisposition`)
- `autonomyVocabUri` (`java.lang.String`)
- `registry` (`io.casehub.eidos.api.VocabularyRegistry`)

### `public static java.util.OptionalLong latencyBound(io.casehub.eidos.api.AgentCapability capability)`

#### Parameters

- `capability` (`io.casehub.eidos.api.AgentCapability`)

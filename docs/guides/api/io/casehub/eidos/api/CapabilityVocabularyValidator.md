# io.casehub.eidos.api.CapabilityVocabularyValidator

**Package:** `io.casehub.eidos.api`

**Kind:** `class`

Utility for validating capability vocabularies in agent descriptors.

## Constructors

### `private CapabilityVocabularyValidator()`

## Methods

### `public static void validate(io.casehub.eidos.api.AgentDescriptor descriptor, io.casehub.eidos.api.VocabularyRegistry vocabularyRegistry)`

Validates all capability vocabularies in the descriptor.

#### Parameters

- `descriptor` (`io.casehub.eidos.api.AgentDescriptor`) — agent descriptor to validate
- `vocabularyRegistry` (`io.casehub.eidos.api.VocabularyRegistry`) — registry to check vocabularies against

#### Throws

- `AgentValidationException` — if any vocabulary is not registered or any capability name is not a valid term

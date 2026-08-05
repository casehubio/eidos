# io.casehub.eidos.api.AgentCapability

**Package:** `io.casehub.eidos.api`

**Kind:** `record`

A declared capability of an agent with operational metadata and epistemic domain qualifications.
qualityHint is a self-declared prior — ActorTrustScore per CapabilityTag in casehub-ledger
is the evidence-backed replacement that accumulates over time.

## Fields

### `capabilityVocabulary` (`java.lang.String`)

### `costHint` (`java.lang.String`)

### `description` (`java.lang.String`)

### `epistemicDomains` (`java.util.Map<java.lang.String,java.lang.Double>`)

### `excludedDomains` (`java.util.Set<java.lang.String>`)

### `inputTypes` (`java.util.List<java.lang.String>`)

### `latencyHintP50Ms` (`java.lang.Long`)

### `name` (`java.lang.String`)

### `outputTypes` (`java.util.List<java.lang.String>`)

### `qualityHint` (`java.lang.Double`)

### `tags` (`java.util.List<java.lang.String>`)

## Record Components

### `capabilityVocabulary` (`java.lang.String`)

### `costHint` (`java.lang.String`)

### `description` (`java.lang.String`)

### `epistemicDomains` (`java.util.Map<java.lang.String,java.lang.Double>`)

### `excludedDomains` (`java.util.Set<java.lang.String>`)

### `inputTypes` (`java.util.List<java.lang.String>`)

### `latencyHintP50Ms` (`java.lang.Long`)

### `name` (`java.lang.String`)

### `outputTypes` (`java.util.List<java.lang.String>`)

### `qualityHint` (`java.lang.Double`)

### `tags` (`java.util.List<java.lang.String>`)

## Constructors

### `public AgentCapability(java.lang.String name, java.lang.String description, java.lang.String capabilityVocabulary, java.lang.Double qualityHint, java.lang.Long latencyHintP50Ms, java.lang.String costHint, java.util.List<java.lang.String> inputTypes, java.util.List<java.lang.String> outputTypes, java.util.List<java.lang.String> tags, java.util.Map<java.lang.String,java.lang.Double> epistemicDomains, java.util.Set<java.lang.String> excludedDomains)`

#### Parameters

- `name` (`java.lang.String`)
- `description` (`java.lang.String`)
- `capabilityVocabulary` (`java.lang.String`)
- `qualityHint` (`java.lang.Double`)
- `latencyHintP50Ms` (`java.lang.Long`)
- `costHint` (`java.lang.String`)
- `inputTypes` (`java.util.List<java.lang.String>`)
- `outputTypes` (`java.util.List<java.lang.String>`)
- `tags` (`java.util.List<java.lang.String>`)
- `epistemicDomains` (`java.util.Map<java.lang.String,java.lang.Double>`)
- `excludedDomains` (`java.util.Set<java.lang.String>`)

## Methods

### `public static io.casehub.eidos.api.AgentCapability.Builder builder()`

### `public java.lang.String capabilityVocabulary()`

### `public java.lang.String costHint()`

### `public java.lang.String description()`

### `public java.util.Map<java.lang.String,java.lang.Double> epistemicDomains()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public java.util.Set<java.lang.String> excludedDomains()`

### `public final int hashCode()`

### `public java.util.List<java.lang.String> inputTypes()`

### `public java.lang.Long latencyHintP50Ms()`

### `public java.lang.String name()`

### `public java.util.List<java.lang.String> outputTypes()`

### `public java.lang.Double qualityHint()`

### `public java.util.List<java.lang.String> tags()`

### `public final java.lang.String toString()`

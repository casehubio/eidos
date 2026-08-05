# io.casehub.eidos.api.VocabularyRegistry

**Package:** `io.casehub.eidos.api`

**Kind:** `interface`

## Methods

### `public abstract java.util.List<? extends io.casehub.eidos.api.VocabularyTerm> allTerms(java.lang.String vocabUri)`

Returns terms in enum declaration order. Empty list if URI not registered.

#### Parameters

- `vocabUri` (`java.lang.String`)

### `public abstract java.util.List<? extends io.casehub.eidos.api.VocabularyTerm> ancestors(java.lang.String vocabUri, java.lang.String value)`

Returns all ancestors (more general terms) of `value` in the vocabulary hierarchy.

#### Parameters

- `vocabUri` (`java.lang.String`) — vocabulary URI
- `value` (`java.lang.String`) — term to find ancestors for

#### Returns

list of ancestor terms ordered by depth (closest first), or empty if none

### `public abstract java.util.List<? extends io.casehub.eidos.api.VocabularyTerm> descendants(java.lang.String vocabUri, java.lang.String value)`

Returns all descendants (more specific terms) of `value` in the vocabulary hierarchy.

#### Parameters

- `vocabUri` (`java.lang.String`) — vocabulary URI
- `value` (`java.lang.String`) — term to find descendants for

#### Returns

list of descendant terms ordered by depth (closest first), or empty if none

### `public abstract java.util.Optional<T> equivalentValues(S from, java.lang.Class<T> targetVocab)`

Returns the equivalent constant in `targetVocab` via `VocabularyTerm.exactMatch`.
Does NOT require registration — delegates directly to the source constant's method.

#### Parameters

- `from` (`S`)
- `targetVocab` (`java.lang.Class<T>`)

### `public abstract java.util.Optional<T> equivalentValues(S from, java.lang.Class<T> targetVocab, io.casehub.eidos.api.DispositionAxis axis)`

Returns the axis-scoped equivalent constant via `VocabularyTerm.axisExactMatch`.
Does NOT require registration — delegates directly to the source constant's method.

#### Parameters

- `from` (`S`)
- `targetVocab` (`java.lang.Class<T>`)
- `axis` (`io.casehub.eidos.api.DispositionAxis`)

### `public abstract java.util.Optional<java.lang.String> equivalentValues(java.lang.String fromUri, java.lang.String value, java.lang.String toUri)`

#### Parameters

- `fromUri` (`java.lang.String`)
- `value` (`java.lang.String`)
- `toUri` (`java.lang.String`)

### `public abstract java.util.Optional<java.lang.String> equivalentValues(java.lang.String fromUri, java.lang.String value, java.lang.String toUri, io.casehub.eidos.api.DispositionAxis axis)`

#### Parameters

- `fromUri` (`java.lang.String`)
- `value` (`java.lang.String`)
- `toUri` (`java.lang.String`)
- `axis` (`io.casehub.eidos.api.DispositionAxis`)

### `public abstract java.util.Map<java.lang.String,java.util.Set<java.lang.String>> expandForMatchingByVocabulary(java.lang.String value)`

Expands a value to all related terms (ancestors and descendants) grouped by vocabulary.
Used by `AgentRegistry.find()` to match vocabulary-grounded capabilities.

#### Parameters

- `value` (`java.lang.String`) — term to expand (primary value, not alias)

#### Returns

map of vocabulary URI → expanded term set, or empty map if value is not registered in any vocabulary

### `public abstract boolean isRegistered(java.lang.String vocabUri)`

#### Parameters

- `vocabUri` (`java.lang.String`)

### `public abstract io.casehub.eidos.api.MatchDegree match(java.lang.String vocabUri, java.lang.String declaredValue, java.lang.String requestedValue)`

Computes the OWLS-MX match degree between a declared capability and a requested capability.

#### Parameters

- `vocabUri` (`java.lang.String`) — vocabulary URI grounding both values
- `declaredValue` (`java.lang.String`) — value declared in agent descriptor
- `requestedValue` (`java.lang.String`) — value requested at probe/query time

#### Returns

Exact, Plugin (declared subsumes requested), Specialization (declared is subsumed by requested), or None

### `public abstract void register(java.lang.Class<T> vocab)`

Registers a vocabulary enum. The class must carry `VocabularyMetadata`.

#### Parameters

- `vocab` (`java.lang.Class<T>`)

#### Throws

- `IllegalArgumentException` — if the vocabulary URI is blank (annotation
        attributes cannot be null at runtime — blank is the only invalid state),
        if the vocabulary has no constants, if value/alias conflicts exist within
        the vocabulary, or if a different vocabulary is already registered under
        the same URI.

### `public abstract java.util.Set<java.lang.String> registeredUris()`

### `public abstract java.util.Optional<T> resolve(java.lang.Class<T> vocab, java.lang.String value)`

Resolves `value` (primary or alias) to a typed constant.
REQUIRES the vocabulary to be registered — uses the internal byClass index.

#### Parameters

- `vocab` (`java.lang.Class<T>`)
- `value` (`java.lang.String`)

### `public abstract java.util.Optional<? extends io.casehub.eidos.api.VocabularyTerm> resolve(java.lang.String vocabUri, java.lang.String value)`

#### Parameters

- `vocabUri` (`java.lang.String`)
- `value` (`java.lang.String`)

### `public abstract boolean subsumes(java.lang.String vocabUri, java.lang.String generalValue, java.lang.String specificValue)`

Tests whether `generalValue` subsumes `specificValue` in the given vocabulary.

#### Parameters

- `vocabUri` (`java.lang.String`) — vocabulary URI
- `generalValue` (`java.lang.String`) — more general term
- `specificValue` (`java.lang.String`) — more specific term (candidate descendant)

#### Returns

true if `specificValue` specializes `generalValue`, or they are equal

### `public abstract java.util.Optional<io.casehub.eidos.api.VocabularyMetadata> vocabularyMetadata(java.lang.String uri)`

Returns the vocabulary-level metadata annotation for the given URI.
Empty if the URI is not registered.
See `VocabularyMetadata` for field semantics — `name()`,
`version()`, and `description()` default to `""`,
meaning "not provided"; callers should treat `isEmpty()` as absent.

#### Parameters

- `uri` (`java.lang.String`)

# io.casehub.eidos.api.VocabularyTerm

**Package:** `io.casehub.eidos.api`

**Kind:** `interface`

A term within a vocabulary. Implemented by enum constants.

<p>`.exactMatch` and `.axisExactMatch` are independent. A term may implement
either, both, or neither. The registry routes axis-aware and axis-unaware lookups to the
appropriate method independently — calling the axis-unaware overload against a DISC term
(which only implements axisExactMatch) returns `Optional.empty()`, which is correct.

## Methods

### `public default java.util.List<java.lang.String> aliases()`

### `public default java.lang.String antiPatternWarning()`

### `public default java.util.Optional<io.casehub.eidos.api.VocabularyTerm> axisExactMatch(java.lang.Class<?> targetVocab, io.casehub.eidos.api.DispositionAxis axis)`

Axis-aware cross-vocabulary equivalence.

<p>Implementations covering a given `targetVocab` MUST use an exhaustive switch
on `axis` with no default branch — adding a new `DispositionAxis` value
then causes a compile error, forcing explicit coverage of the new axis.
`Optional.empty()` is a valid branch for axes with no meaningful mapping.
Do NOT wrap the switch in `Optional.of()` — that forbids gaps.

<p>The exhaustive switch enforces completeness for the axis dimension only. Adding a
new target vocabulary requires a new `if (targetVocab == ...)` branch;
no compile-time enforcement exists for the target-vocabulary dimension.

#### Parameters

- `targetVocab` (`java.lang.Class<?>`)
- `axis` (`io.casehub.eidos.api.DispositionAxis`)

### `public default java.util.List<io.casehub.eidos.api.DispositionValue> defaultProfile()`

### `public default java.lang.String description()`

Returns `""` when no description was provided. Callers treat `isEmpty()` as absent.

### `public default java.util.Optional<io.casehub.eidos.api.VocabularyTerm> exactMatch(java.lang.Class<?> targetVocab)`

Axis-unaware cross-vocabulary equivalence.
Returns the equivalent constant in `targetVocab`, or empty if none.
The registry's typed overload calls `targetVocab.cast()` on the result.

#### Parameters

- `targetVocab` (`java.lang.Class<?>`)

### `public default boolean impliesSupervision()`

Returns `true` if this term indicates the entity operates under
supervision and should escalate to a supervisor when encountering
uncertain or high-stakes decisions.

<p>Primarily meaningful for AUTONOMY axis
terms. Slot terms, capability terms, and other axis terms inherit the
default `false` — semantically correct and harmless if called in a
non-autonomy context.

<p>Used by `BehavioralExpectations.escalationExpected` to determine
whether escalation compliance should be monitored for an agent.

### `public abstract java.lang.String label()`

### `public default java.util.Optional<io.casehub.eidos.api.VocabularyTerm> opposite()`

### `public default java.lang.String responseStyleGuidance()`

### `public default java.util.List<io.casehub.eidos.api.VocabularyTerm> specializes()`

### `public abstract java.lang.String value()`

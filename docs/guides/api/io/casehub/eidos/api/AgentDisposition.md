# io.casehub.eidos.api.AgentDisposition

**Package:** `io.casehub.eidos.api`

**Kind:** `record`

## Fields

### `autonomy` (`java.util.List<io.casehub.eidos.api.DispositionValue>`)

### `conflictMode` (`java.util.List<io.casehub.eidos.api.DispositionValue>`)

### `delegation` (`boolean`)

### `dispositionProfile` (`java.util.List<io.casehub.eidos.api.DispositionValue>`)

### `riskAppetite` (`java.util.List<io.casehub.eidos.api.DispositionValue>`)

### `ruleFollowing` (`java.util.List<io.casehub.eidos.api.DispositionValue>`)

### `socialOrient` (`java.util.List<io.casehub.eidos.api.DispositionValue>`)

## Record Components

### `autonomy` (`java.util.List<io.casehub.eidos.api.DispositionValue>`)

### `conflictMode` (`java.util.List<io.casehub.eidos.api.DispositionValue>`)

### `delegation` (`boolean`)

### `dispositionProfile` (`java.util.List<io.casehub.eidos.api.DispositionValue>`)

### `riskAppetite` (`java.util.List<io.casehub.eidos.api.DispositionValue>`)

### `ruleFollowing` (`java.util.List<io.casehub.eidos.api.DispositionValue>`)

### `socialOrient` (`java.util.List<io.casehub.eidos.api.DispositionValue>`)

## Constructors

### `public AgentDisposition(java.util.List<io.casehub.eidos.api.DispositionValue> socialOrient, java.util.List<io.casehub.eidos.api.DispositionValue> ruleFollowing, java.util.List<io.casehub.eidos.api.DispositionValue> riskAppetite, java.util.List<io.casehub.eidos.api.DispositionValue> autonomy, java.util.List<io.casehub.eidos.api.DispositionValue> conflictMode, boolean delegation, java.util.List<io.casehub.eidos.api.DispositionValue> dispositionProfile)`

#### Parameters

- `socialOrient` (`java.util.List<io.casehub.eidos.api.DispositionValue>`)
- `ruleFollowing` (`java.util.List<io.casehub.eidos.api.DispositionValue>`)
- `riskAppetite` (`java.util.List<io.casehub.eidos.api.DispositionValue>`)
- `autonomy` (`java.util.List<io.casehub.eidos.api.DispositionValue>`)
- `conflictMode` (`java.util.List<io.casehub.eidos.api.DispositionValue>`)
- `delegation` (`boolean`)
- `dispositionProfile` (`java.util.List<io.casehub.eidos.api.DispositionValue>`)

## Methods

### `public java.util.List<io.casehub.eidos.api.DispositionValue> autonomy()`

### `public static io.casehub.eidos.api.AgentDisposition.Builder builder()`

### `public java.util.List<io.casehub.eidos.api.DispositionValue> conflictMode()`

### `public boolean delegation()`

### `public java.util.List<io.casehub.eidos.api.DispositionValue> dispositionProfile()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public java.util.List<io.casehub.eidos.api.DispositionValue> get(io.casehub.eidos.api.DispositionAxis axis)`

#### Parameters

- `axis` (`io.casehub.eidos.api.DispositionAxis`)

### `public final int hashCode()`

### `public java.lang.String primaryTerm(io.casehub.eidos.api.DispositionAxis axis)`

#### Parameters

- `axis` (`io.casehub.eidos.api.DispositionAxis`)

### `public java.util.List<io.casehub.eidos.api.DispositionValue> riskAppetite()`

### `public java.util.List<io.casehub.eidos.api.DispositionValue> ruleFollowing()`

### `private static java.util.List<io.casehub.eidos.api.DispositionValue> sanitizeAxis(java.util.List<io.casehub.eidos.api.DispositionValue> values)`

#### Parameters

- `values` (`java.util.List<io.casehub.eidos.api.DispositionValue>`)

### `public java.util.List<io.casehub.eidos.api.DispositionValue> socialOrient()`

### `public final java.lang.String toString()`

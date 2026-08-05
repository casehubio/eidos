# io.casehub.eidos.api.GraphDataSufficiency

**Package:** `io.casehub.eidos.api`

**Kind:** `record`

## Fields

### `dataFrom` (`java.time.Instant`)

### `dataThrough` (`java.time.Instant`)

### `level` (`io.casehub.eidos.api.SufficiencyLevel`)

### `sampleCount` (`int`)

### `warnings` (`java.util.List<java.lang.String>`)

## Record Components

### `dataFrom` (`java.time.Instant`)

### `dataThrough` (`java.time.Instant`)

### `level` (`io.casehub.eidos.api.SufficiencyLevel`)

### `sampleCount` (`int`)

### `warnings` (`java.util.List<java.lang.String>`)

## Constructors

### `public GraphDataSufficiency(int sampleCount, io.casehub.eidos.api.SufficiencyLevel level, java.time.Instant dataFrom, java.time.Instant dataThrough, java.util.List<java.lang.String> warnings)`

#### Parameters

- `sampleCount` (`int`)
- `level` (`io.casehub.eidos.api.SufficiencyLevel`)
- `dataFrom` (`java.time.Instant`)
- `dataThrough` (`java.time.Instant`)
- `warnings` (`java.util.List<java.lang.String>`)

## Methods

### `public java.time.Instant dataFrom()`

### `public java.time.Instant dataThrough()`

### `public static io.casehub.eidos.api.GraphDataSufficiency empty(java.util.List<java.lang.String> warnings)`

#### Parameters

- `warnings` (`java.util.List<java.lang.String>`)

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public static io.casehub.eidos.api.GraphDataSufficiency forCount(int count, java.time.Instant from, java.time.Instant through, java.util.List<java.lang.String> warnings)`

#### Parameters

- `count` (`int`)
- `from` (`java.time.Instant`)
- `through` (`java.time.Instant`)
- `warnings` (`java.util.List<java.lang.String>`)

### `public final int hashCode()`

### `public io.casehub.eidos.api.SufficiencyLevel level()`

### `public int sampleCount()`

### `public final java.lang.String toString()`

### `public java.util.List<java.lang.String> warnings()`

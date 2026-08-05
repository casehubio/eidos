# io.casehub.eidos.api.BackfillResult

**Package:** `io.casehub.eidos.api`

**Kind:** `record`

## Fields

### `imported` (`int`)

### `rangeFrom` (`java.time.Instant`)

### `rangeThrough` (`java.time.Instant`)

### `skipped` (`int`)

## Record Components

### `imported` (`int`)

### `rangeFrom` (`java.time.Instant`)

### `rangeThrough` (`java.time.Instant`)

### `skipped` (`int`)

## Constructors

### `public BackfillResult(int imported, int skipped, java.time.Instant rangeFrom, java.time.Instant rangeThrough)`

#### Parameters

- `imported` (`int`)
- `skipped` (`int`)
- `rangeFrom` (`java.time.Instant`)
- `rangeThrough` (`java.time.Instant`)

## Methods

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public int imported()`

### `public java.time.Instant rangeFrom()`

### `public java.time.Instant rangeThrough()`

### `public int skipped()`

### `public final java.lang.String toString()`

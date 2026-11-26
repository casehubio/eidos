# io.casehub.eidos.api.DetectedRole

**Package:** `io.casehub.eidos.api`

**Kind:** `record`

## Fields

### `aggregateConfidence` (`double`)

### `effectiveCount` (`double`)

### `firstObserved` (`java.time.Instant`)

### `lastObserved` (`java.time.Instant`)

### `patternTypes` (`java.util.List<java.lang.String>`)

### `roleName` (`java.lang.String`)

### `signalCount` (`int`)

## Record Components

### `aggregateConfidence` (`double`)

### `effectiveCount` (`double`)

### `firstObserved` (`java.time.Instant`)

### `lastObserved` (`java.time.Instant`)

### `patternTypes` (`java.util.List<java.lang.String>`)

### `roleName` (`java.lang.String`)

### `signalCount` (`int`)

## Constructors

### `public DetectedRole(java.lang.String roleName, double aggregateConfidence, int signalCount, double effectiveCount, java.time.Instant firstObserved, java.time.Instant lastObserved, java.util.List<java.lang.String> patternTypes)`

#### Parameters

- `roleName` (`java.lang.String`)
- `aggregateConfidence` (`double`)
- `signalCount` (`int`)
- `effectiveCount` (`double`)
- `firstObserved` (`java.time.Instant`)
- `lastObserved` (`java.time.Instant`)
- `patternTypes` (`java.util.List<java.lang.String>`)

## Methods

### `public double aggregateConfidence()`

### `public double effectiveCount()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public java.time.Instant firstObserved()`

### `public final int hashCode()`

### `public java.time.Instant lastObserved()`

### `public java.util.List<java.lang.String> patternTypes()`

### `public java.lang.String roleName()`

### `public int signalCount()`

### `public final java.lang.String toString()`

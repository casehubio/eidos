# io.casehub.eidos.api.RoleChangePolicy

**Package:** `io.casehub.eidos.api`

**Kind:** `record`

## Fields

### `ADVISORY` (`io.casehub.eidos.api.RoleChangePolicy`)

### `DISABLED` (`io.casehub.eidos.api.RoleChangePolicy`)

### `demotionConfidence` (`double`)

### `minConfidence` (`double`)

### `minHoldCycles` (`int`)

### `mode` (`io.casehub.eidos.api.RoleChangePolicy.Mode`)

## Record Components

### `demotionConfidence` (`double`)

### `minConfidence` (`double`)

### `minHoldCycles` (`int`)

### `mode` (`io.casehub.eidos.api.RoleChangePolicy.Mode`)

## Constructors

### `public RoleChangePolicy(io.casehub.eidos.api.RoleChangePolicy.Mode mode, double minConfidence, double demotionConfidence, int minHoldCycles)`

#### Parameters

- `mode` (`io.casehub.eidos.api.RoleChangePolicy.Mode`)
- `minConfidence` (`double`)
- `demotionConfidence` (`double`)
- `minHoldCycles` (`int`)

## Methods

### `public double demotionConfidence()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public double minConfidence()`

### `public int minHoldCycles()`

### `public io.casehub.eidos.api.RoleChangePolicy.Mode mode()`

### `public final java.lang.String toString()`
